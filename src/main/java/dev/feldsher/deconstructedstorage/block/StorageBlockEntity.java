package dev.feldsher.deconstructedstorage.block;

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
			}

			@Override
			public int getBaseStackSizeMultiplier() {
				return getStorageBlock() != null ? getStorageBlock().getBaseStackSizeMultiplier() : super.getBaseStackSizeMultiplier();
			}

			@Override
			public ItemStack getWrappedStorageStack() {
				return getStorageBlock() instanceof StorageBlockBase && world != null
						? ((StorageBlockBase) getStorageBlock()).getStorageStack(world, pos, world.getBlockState(pos)) : ItemStack.EMPTY;
			}

			@Override
			public String getStorageType() {
				return StorageBlockEntity.this.getStorageType();
			}

			@Override
			public String getDisplayName() {
				return StorageBlockEntity.this.getStorageDisplayName();
			}
		};
	}

	@Nullable
	private IStorageBlock getStorageBlock() {
		Block block = world != null ? world.getBlockState(pos).getBlock() : getBlockType();
		return block instanceof IStorageBlock ? (IStorageBlock) block : null;
	}

	/** What it is, for the limits of how many upgrades of a kind it takes. */
	protected abstract String getStorageType();

	protected int getNumberOfDisplayItems() {
		return 1;
	}

	@SuppressWarnings("unused") // stack used in overrides
	protected boolean isAllowedInStorage(ItemStack stack) {
		return true;
	}

	public StorageWrapper getStorageWrapper() {
		return storageWrapper;
	}

	/** Saved, and what clients see of it sent to them: colors and the like show at once. */
	private void onWrapperChanged() {
		markDirty();
		notifyBlockUpdate();
	}

	protected void notifyBlockUpdate() {
		if (world != null && !world.isRemote) {
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
		if (world == null || world.isRemote) {
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
	}

	protected void loadSynchronizedData(NBTTagCompound tag) {
		displayName = tag.hasKey(DISPLAY_NAME_TAG) ? tag.getString(DISPLAY_NAME_TAG) : null;
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

	/** Its items, and its upgrades, onto the ground; the contents themselves then go, with nothing left to hold. */
	public void dropContents() {
		if (world == null || world.isRemote || !storageWrapper.getContentsUuid().isPresent()) {
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
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(storageWrapper.getInventoryForInputOutput());
		}
		return super.getCapability(capability, facing);
	}
}
