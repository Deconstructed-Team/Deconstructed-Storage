package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.storage.StorageContents;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A placed storage. It keeps what its contents are, by UUID, and how it looks; the contents are in
 * {@link StorageBlockContents}. It ticks only for upgrades that tick: 1.12 can't take a block entity in and out of the
 * ticking list as 1.21 swaps its ticker, so it asks the upgrades each tick, from a list made again as they change.
 */
public abstract class StorageBlockEntity extends TileEntity implements ITickable {
	public static final String STORAGE_WRAPPER_TAG = "storageWrapper";
	private static final String DISPLAY_NAME_TAG = "displayName";
	private static final int OPENERS_EVENT = 1;

	private final StorageWrapper storageWrapper;
	@Nullable
	protected String displayName = null;
	private int openersCount = 0;
	@Nullable
	private List<ITickableUpgrade> tickableUpgrades = null;
	private boolean isDroppingContents = false;
	private boolean contentsMovedToItem = false;
	private boolean beingUpgraded = false;
	private boolean locked = false;
	private boolean showLock = true;
	private boolean showTier = true;
	private boolean showUpgrades = false;
	private boolean renderInfoDirty = true;
	// kept here: the contents hold it weakly, so that it goes with this block entity
	private final Runnable slotChangeListener = () -> renderInfoDirty = true;
	@Nullable
	private StorageContents listenedContents = null;
	@Nullable
	private LockedItemHandler lockedItemHandler = null;
	@Nullable
	private IBlockState removedState = null;

	protected StorageBlockEntity() {
		storageWrapper = new StorageWrapper(this::onWrapperChanged) {
			@Override
			public int getDefaultNumberOfInventorySlots() {
				return getStorageBlock() != null ? getStorageBlock().getNumberOfInventorySlots() : 0;
			}

			@Override
			public int getDefaultNumberOfUpgradeSlots() {
				return getStorageBlock() != null ? getStorageBlock().getNumberOfUpgradeSlots() : 0;
			}

			@Override
			protected boolean isAllowedInStorage(ItemStack stack) {
				return StorageBlockEntity.this.isAllowedInStorage(stack);
			}

			@Override
			protected int getNumberOfDisplayItems() {
				return StorageBlockEntity.this.getNumberOfDisplayItems();
			}

			@Override
			protected void onUpgradeRefresh() {
				tickableUpgrades = null;
				// the front shows the upgrades
				renderInfoDirty = true;
			}

			@Override
			public int getBaseStackSizeMultiplier() {
				return getStorageBlock() != null ? getStorageBlock().getBaseStackSizeMultiplier() : super.getBaseStackSizeMultiplier();
			}

			@Override
			public ItemStack getWrappedStorageStack() {
				return getStorageBlock() instanceof StorageBlockBase && world != null
						? ((StorageBlockBase) getStorageBlock()).getStorageStack(world, pos, getBlockState()) : ItemStack.EMPTY;
			}

			@Override
			public String getStorageType() {
				return StorageBlockEntity.this.getStorageType();
			}

			@Override
			public String getDisplayName() {
				return StorageBlockEntity.this.getStorageDisplayName();
			}

			@Override
			protected boolean showsCountsAndFillLevels() {
				return StorageBlockEntity.this.showsCountsAndFillLevels();
			}
		};
	}

	@Nullable
	private IStorageBlock getStorageBlock() {
		Block block = getBlockState().getBlock();
		return block instanceof IStorageBlock ? (IStorageBlock) block : null;
	}

	/**
	 * Its block's state; while it's being broken, the one it had, which the world no longer has: what's left to do then,
	 * as dropping its items, still needs to know what it was.
	 */
	public IBlockState getBlockState() {
		if (removedState != null) {
			return removedState;
		}
		return world != null ? world.getBlockState(pos) : net.minecraft.init.Blocks.AIR.getDefaultState();
	}

	/** Called as its block is broken, before its items drop. */
	public void setRemovedState(IBlockState removedState) {
		this.removedState = removedState;
	}

	/** What it is, for the limits of how many upgrades of a kind it takes. */
	protected abstract String getStorageType();

	protected int getNumberOfDisplayItems() {
		return 1;
	}

	/** Whether its render info has every slot's count and fill ratio, for its front to show. */
	protected boolean showsCountsAndFillLevels() {
		return false;
	}

	@SuppressWarnings("unused") // stack used in overrides
	protected boolean isAllowedInStorage(ItemStack stack) {
		return true;
	}

	public StorageWrapper getStorageWrapper() {
		return storageWrapper;
	}

	/** The block entity whose storage this is part of: itself, but for the second half of a double chest. */
	public StorageBlockEntity getMainBlockEntity() {
		return this;
	}

	/** The storage this block is part of, that its container, pipes and comparators work on. */
	public StorageWrapper getMainStorageWrapper() {
		return getMainBlockEntity().getStorageWrapper();
	}

	/** Saved, and what clients see of it sent to them: colors and the like show at once. */
	private void onWrapperChanged() {
		markDirty();
		notifyBlockUpdate();
	}

	/** The server sends clients the change; a client, which changed its own copy, as a block it placed, draws it again. */
	protected void notifyBlockUpdate() {
		if (world == null) {
			return;
		}
		if (world.isRemote) {
			world.markBlockRangeForRenderUpdate(pos, pos);
		} else {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	public String getStorageDisplayName() {
		return displayName != null ? displayName : getBlockType().getLocalizedName();
	}

	public boolean hasCustomName() {
		return displayName != null;
	}

	public void setCustomName(@Nullable String customName) {
		displayName = customName;
		markDirty();
	}

	@Override
	public void update() {
		if (world == null) {
			return;
		}
		animationTick();
		if (world.isRemote) {
			clientTick();
			return;
		}
		refreshRenderInfoIfChanged();
		if (!canTickUpgrades()) {
			return;
		}
		if (tickableUpgrades == null) {
			// none until the contents exist: a storage nobody opened has no upgrades
			tickableUpgrades = storageWrapper.getContentsUuid().isPresent()
					? storageWrapper.getUpgradeHandler().getWrappersThatImplement(ITickableUpgrade.class) : java.util.Collections.emptyList();
		}
		for (ITickableUpgrade upgrade : tickableUpgrades) {
			upgrade.tick(null, world, pos);
		}
	}

	/**
	 * What clients draw it with follows its items: told of each slot change by the contents, it builds that anew the
	 * tick after. Open, its container refreshes it every tick as settings change too.
	 */
	private void refreshRenderInfoIfChanged() {
		if (!storageWrapper.getContentsUuid().isPresent() || getMainBlockEntity() != this) {
			return;
		}
		StorageContents contents = storageWrapper.getContents();
		if (listenedContents != contents) {
			contents.addSlotChangeListener(this, slotChangeListener);
			listenedContents = contents;
			renderInfoDirty = true;
		}
		if (renderInfoDirty) {
			renderInfoDirty = false;
			storageWrapper.refreshRenderInfo();
		}
	}

	/** Whether its upgrades work now: not in a packed storage. */
	protected boolean canTickUpgrades() {
		return true;
	}

	/** What moves on both sides, as a shulker box's lid, which pushes entities out of its way. */
	protected void animationTick() {
		//noop by default
	}

	/** What it animates, as a chest its lid. */
	protected void clientTick() {
		//noop by default
	}

	/** Upgrades went in or out through something that isn't this block's container, as another stack with these contents. */
	public void refreshTickableUpgrades() {
		tickableUpgrades = null;
	}

	public boolean isOpen() {
		return openersCount > 0;
	}

	public void startOpen(EntityPlayer player) {
		if (!player.isSpectator() && world != null && !world.isRemote) {
			if (openersCount++ == 0) {
				onOpen();
			}
			world.addBlockEvent(pos, getBlockType(), OPENERS_EVENT, openersCount);
		}
	}

	public void stopOpen(EntityPlayer player) {
		if (!player.isSpectator() && world != null && !world.isRemote && openersCount > 0) {
			if (--openersCount == 0) {
				onClose();
			}
			world.addBlockEvent(pos, getBlockType(), OPENERS_EVENT, openersCount);
		}
	}

	protected void onOpen() {
		//noop by default
	}

	protected void onClose() {
		//noop by default
	}

	@Override
	public boolean receiveClientEvent(int id, int type) {
		if (id == OPENERS_EVENT) {
			boolean wasOpen = isOpen();
			openersCount = type;
			if (world != null && world.isRemote && wasOpen != isOpen()) {
				world.markBlockRangeForRenderUpdate(pos, pos);
			}
			return true;
		}
		return super.receiveClientEvent(id, type);
	}

	protected void playSound(@Nullable SoundEvent sound, EnumFacing facing) {
		if (world == null || sound == null) {
			return;
		}
		double x = pos.getX() + 0.5D + facing.getXOffset() / 2.0D;
		double y = pos.getY() + 0.5D + facing.getYOffset() / 2.0D;
		double z = pos.getZ() + 0.5D + facing.getZOffset() / 2.0D;
		world.playSound(null, x, y, z, sound, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		storageWrapper.load(compound.getCompoundTag(STORAGE_WRAPPER_TAG));
		loadSynchronizedData(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setTag(STORAGE_WRAPPER_TAG, storageWrapper.save(new NBTTagCompound()));
		saveSynchronizedData(compound);
		return compound;
	}

	protected void saveSynchronizedData(NBTTagCompound tag) {
		if (displayName != null) {
			tag.setString(DISPLAY_NAME_TAG, displayName);
		}
		if (locked) {
			tag.setBoolean("locked", true);
		}
		if (!showLock) {
			tag.setBoolean("showLock", false);
		}
		if (!showTier) {
			tag.setBoolean("showTier", false);
		}
		if (showUpgrades) {
			tag.setBoolean("showUpgrades", true);
		}
	}

	protected void loadSynchronizedData(NBTTagCompound tag) {
		displayName = tag.hasKey(DISPLAY_NAME_TAG) ? tag.getString(DISPLAY_NAME_TAG) : null;
		locked = tag.getBoolean("locked");
		showLock = !tag.hasKey("showLock") || tag.getBoolean("showLock");
		showTier = !tag.hasKey("showTier") || tag.getBoolean("showTier");
		showUpgrades = tag.getBoolean("showUpgrades");
	}

	/**
	 * These toggles are the main block's, of a double chest the main half's: a second half passes them on, and is
	 * drawn by them.
	 *
	 * Locked, a storage takes from pipes and the like only what it already holds, or what its memorized slots are set
	 * for: no new kind of item gets in by itself. Players put in anything still.
	 */
	public boolean isLocked() {
		return locked;
	}

	public void toggleLock() {
		StorageBlockEntity main = getMainBlockEntity();
		if (main != this) {
			main.toggleLock();
			return;
		}
		locked = !locked;
		lockedItemHandler = null;
		if (memorizesItemsWhenLocked()) {
			MemorySettingsCategory memorySettings = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			if (locked) {
				memorySettings.selectSlots(0, storageWrapper.getInventoryHandler().getSlots());
			} else {
				memorySettings.unselectAllSlots();
			}
			storageWrapper.getInventoryHandler().onSlotFiltersChanged();
			renderInfoDirty = true;
		}
		onToggled();
	}

	/** Locked, its slots remember what they hold, as a limited barrel's do. */
	public boolean memorizesItemsWhenLocked() {
		return false;
	}

	/** Locked, whether an empty slot takes in what another slot holds; a limited barrel's slots each keep to their own item. */
	public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
		return true;
	}

	public boolean shouldShowLock() {
		return showLock;
	}

	public void toggleLockVisibility() {
		StorageBlockEntity main = getMainBlockEntity();
		if (main != this) {
			main.toggleLockVisibility();
			return;
		}
		showLock = !showLock;
		onToggled();
	}

	public boolean shouldShowTier() {
		return showTier;
	}

	public void toggleTierVisibility() {
		StorageBlockEntity main = getMainBlockEntity();
		if (main != this) {
			main.toggleTierVisibility();
			return;
		}
		showTier = !showTier;
		onToggled();
	}

	public boolean shouldShowUpgrades() {
		return showUpgrades;
	}

	public void toggleUpgradesVisibility() {
		StorageBlockEntity main = getMainBlockEntity();
		if (main != this) {
			main.toggleUpgradesVisibility();
			return;
		}
		showUpgrades = !showUpgrades;
		onToggled();
	}

	private void onToggled() {
		markDirty();
		notifyBlockUpdate();
	}

	/** What clients get of it: all but where the contents' handlers are, which the contents bring. */
	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag(STORAGE_WRAPPER_TAG, storageWrapper.save(new NBTTagCompound()));
		saveSynchronizedData(tag);
		return new SPacketUpdateTileEntity(pos, 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		NBTTagCompound tag = pkt.getNbtCompound();
		storageWrapper.load(tag.getCompoundTag(STORAGE_WRAPPER_TAG));
		loadSynchronizedData(tag);
		if (world != null) {
			world.markBlockRangeForRenderUpdate(pos, pos);
		}
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	/**
	 * Its block is being swapped for another tier's, which takes over its data: the old one isn't broken, its items not
	 * dropped, its contents not removed.
	 */
	public void setBeingUpgraded() {
		beingUpgraded = true;
	}

	public boolean isBeingUpgraded() {
		return beingUpgraded;
	}

	/** Its contents went into the stack it dropped as, as a shulker box's: they're not to be dropped, nor removed. */
	public void setContentsMovedToItem() {
		contentsMovedToItem = true;
	}

	public boolean isContentsMovedToItem() {
		return contentsMovedToItem;
	}

	/** Its items, and its upgrades, onto the ground; the contents themselves then go, with nothing left to hold. */
	public void dropContents() {
		if (world == null || world.isRemote || contentsMovedToItem || beingUpgraded || !storageWrapper.getContentsUuid().isPresent()) {
			return;
		}
		isDroppingContents = true;
		InventoryHelper.dropItems(storageWrapper.getInventoryHandler(), world, pos);
		InventoryHelper.dropItems(storageWrapper.getUpgradeHandler(), world, pos);
		isDroppingContents = false;
		storageWrapper.removeContents();
	}

	public boolean isDroppingContents() {
		return isDroppingContents;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			StorageBlockEntity main = getMainBlockEntity();
			if (main.isLocked()) {
				if (main.lockedItemHandler == null) {
					StorageWrapper mainWrapper = main.getStorageWrapper();
					main.lockedItemHandler = new LockedItemHandler(mainWrapper::getInventoryForInputOutput, mainWrapper::getInventoryHandler,
							() -> mainWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class), main.allowsEmptySlotsMatchingItemInsertsWhenLocked());
				}
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(main.lockedItemHandler);
			}
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(main.getStorageWrapper().getInventoryForInputOutput());
		}
		return super.getCapability(capability, facing);
	}
}
