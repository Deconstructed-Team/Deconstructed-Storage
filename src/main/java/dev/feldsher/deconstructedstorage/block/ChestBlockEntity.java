package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.renderdata.DisplaySide;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A placed chest. Two side by side make a double chest: one half, the main one, holds the storage of both, its
 * inventory twice as big; the other only knows where the main one is, and has no contents of its own. They join as a
 * chest is placed beside another and part as one of them goes, which takes its half of the items with it.
 */
public class ChestBlockEntity extends WoodStorageBlockEntity {
	public static final String STORAGE_TYPE = "chest";
	private static final String DOUBLE_MAIN_POS_TAG = "doubleMainPos";

	@Nullable
	private BlockPos doubleMainPos = null;
	private boolean destroyedByPlayer = false;
	private float lidOpenness = 0;
	private float prevLidOpenness = 0;

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	public boolean isMainChest() {
		return doubleMainPos == null;
	}

	@Override
	public StorageBlockEntity getMainBlockEntity() {
		if (doubleMainPos != null && world != null) {
			TileEntity te = world.getTileEntity(doubleMainPos);
			if (te instanceof ChestBlockEntity) {
				return (ChestBlockEntity) te;
			}
		}
		return this;
	}

	public void setMainPos(BlockPos doubleMainPos) {
		this.doubleMainPos = doubleMainPos;
		markDirty();
	}

	public void removeDoubleMainPos() {
		doubleMainPos = null;
		markDirty();
	}

	public void setDestroyedByPlayer() {
		destroyedByPlayer = true;
	}

	@Nullable
	private ChestType getChestType() {
		IBlockState state = getBlockState();
		return state.getBlock() instanceof ChestBlock ? state.getValue(ChestBlock.TYPE) : null;
	}

	@Nullable
	private ChestBlockEntity getOtherPart() {
		if (world == null) {
			return null;
		}
		IBlockState state = getBlockState();
		if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
			return null;
		}
		TileEntity te = world.getTileEntity(pos.offset(ChestBlock.getConnectedDirection(state)));
		return te instanceof ChestBlockEntity ? (ChestBlockEntity) te : null;
	}

	/**
	 * This chest becomes the second half of mainBE's double chest: the main one grows to twice a chest's slots, and
	 * this one's items and slot settings move into the new ones. Its own contents are then gone.
	 */
	public void joinWithChest(ChestBlockEntity mainBE) {
		setMainPos(mainBE.getPos());
		StorageWrapper mainWrapper = mainBE.getStorageWrapper();
		InventoryHandler mainInventoryHandler = mainWrapper.getInventoryHandler();
		int originalNumberOfSlots = mainInventoryHandler.getSlots();
		Block mainBlock = mainBE.getBlockState().getBlock();
		int singleSlots = mainBlock instanceof StorageBlockBase ? ((StorageBlockBase) mainBlock).getNumberOfInventorySlots() : 0;
		mainWrapper.changeSize(2 * singleSlots - originalNumberOfSlots, 0);

		if (getStorageWrapper().getContentsUuid().isPresent()) {
			InventoryHandler thisInventoryHandler = getStorageWrapper().getInventoryHandler();
			for (int slot = 0; slot < thisInventoryHandler.getSlots() && slot + originalNumberOfSlots < mainInventoryHandler.getSlots(); slot++) {
				ItemStack slotStack = thisInventoryHandler.getSlotStack(slot);
				if (!slotStack.isEmpty()) {
					mainInventoryHandler.setStackInSlot(slot + originalNumberOfSlots, slotStack);
				}
			}
			copySettings(this, mainBE, 0, originalNumberOfSlots);
			getStorageWrapper().removeContents();
		}
		mainBE.notifyBlockUpdate();
	}

	private static void copySettings(ChestBlockEntity from, ChestBlockEntity to, int startFromSlot, int slotOffset) {
		SettingsHandler toSettings = to.getStorageWrapper().getSettingsHandler();
		from.getStorageWrapper().getSettingsHandler().getSettingsCategories().forEach((name, category) ->
				copyCategorySettings(category, toSettings.getTypeCategory(getCategoryClass(category)), startFromSlot, slotOffset));
	}

	@SuppressWarnings("unchecked")
	private static <T extends ISettingsCategory<?>> Class<T> getCategoryClass(ISettingsCategory<?> category) {
		return (Class<T>) category.getClass();
	}

	@SuppressWarnings("unchecked")
	private static <T extends ISettingsCategory<?>> void copyCategorySettings(ISettingsCategory<T> category, ISettingsCategory<?> toCategory, int startFromSlot, int slotOffset) {
		category.copyTo((T) toCategory, startFromSlot, slotOffset);
	}

	private static void deleteSettingsFromSlot(ChestBlockEntity from, int startFromSlot) {
		from.getStorageWrapper().getSettingsHandler().getSettingsCategories().forEach((name, category) -> category.deleteSlotSettingsFrom(startFromSlot));
	}

	/**
	 * Broken by a player, a half of a double chest takes its half of the items with it, to drop them, and leaves the
	 * other a chest of its own; broken any other way, as by an explosion, it drops all it holds.
	 */
	@Override
	public void dropContents() {
		ChestType chestType = getChestType();
		if (destroyedByPlayer && chestType != null && chestType != ChestType.SINGLE) {
			if (isMainChest()) {
				moveOtherPartStacksToIt();
			} else {
				moveMyStacksFromMain();
			}
		}
		super.dropContents();
	}

	/** The main half goes: the other gets the second half of the items, as a stack each at most, and is a chest of its own. */
	private void moveOtherPartStacksToIt() {
		ChestBlockEntity other = getOtherPart();
		if (other == null || !getStorageWrapper().getContentsUuid().isPresent()) {
			return;
		}
		other.removeDoubleMainPos();
		InventoryHandler mainInventoryHandler = getStorageWrapper().getInventoryHandler();
		InventoryHandler otherInventoryHandler = other.getStorageWrapper().getInventoryHandler();
		int firstIndex = mainInventoryHandler.getSlots() / 2;
		for (int slot = firstIndex; slot < mainInventoryHandler.getSlots() && slot - firstIndex < otherInventoryHandler.getSlots(); slot++) {
			ItemStack slotStack = mainInventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				otherInventoryHandler.setStackInSlot(slot - firstIndex, slotStack.splitStack(slotStack.getMaxStackSize()));
				mainInventoryHandler.setStackInSlot(slot, slotStack);
			}
		}
		copySettings(this, other, firstIndex, -firstIndex);
		other.notifyBlockUpdate();
	}

	/** The second half goes: it takes the second half of the items out of the main one, which is a chest of its own again. */
	private void moveMyStacksFromMain() {
		StorageBlockEntity mainBE = getMainBlockEntity();
		if (mainBE == this) {
			return;
		}
		StorageWrapper mainWrapper = mainBE.getStorageWrapper();
		InventoryHandler mainInventoryHandler = mainWrapper.getInventoryHandler();
		InventoryHandler thisInventoryHandler = getStorageWrapper().getInventoryHandler();
		int firstIndex = mainInventoryHandler.getSlots() / 2;
		for (int slot = firstIndex; slot < mainInventoryHandler.getSlots() && slot - firstIndex < thisInventoryHandler.getSlots(); slot++) {
			thisInventoryHandler.setStackInSlot(slot - firstIndex, mainInventoryHandler.getSlotStack(slot));
			mainInventoryHandler.setStackInSlot(slot, ItemStack.EMPTY);
		}
		shrinkToSingle((ChestBlockEntity) mainBE);
		removeDoubleMainPos();
	}

	/**
	 * The other half went without taking its items: what's past a single chest's slots drops where it was, and the
	 * inventory shrinks to a single chest's.
	 */
	public void dropSecondPartContents(BlockPos dropPosition) {
		Block block = getBlockState().getBlock();
		if (world == null || !getStorageWrapper().getContentsUuid().isPresent() || !(block instanceof StorageBlockBase)) {
			return;
		}
		InventoryHandler inventoryHandler = getStorageWrapper().getInventoryHandler();
		List<ItemStack> dropItems = new ArrayList<>();
		for (int slot = ((StorageBlockBase) block).getNumberOfInventorySlots(); slot < inventoryHandler.getSlots(); slot++) {
			ItemStack slotStack = inventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				dropItems.add(slotStack.copy());
				inventoryHandler.setStackInSlot(slot, ItemStack.EMPTY);
			}
		}
		for (ItemStack stack : dropItems) {
			while (!stack.isEmpty()) {
				net.minecraft.inventory.InventoryHelper.spawnItemStack(world, dropPosition.getX(), dropPosition.getY(), dropPosition.getZ(), stack.splitStack(stack.getMaxStackSize()));
			}
		}
		shrinkToSingle(this);
	}

	private static void shrinkToSingle(ChestBlockEntity chest) {
		Block block = chest.getBlockState().getBlock();
		if (!(block instanceof StorageBlockBase)) {
			return;
		}
		StorageWrapper wrapper = chest.getStorageWrapper();
		int singleSlots = ((StorageBlockBase) block).getNumberOfInventorySlots();
		int slots = wrapper.getInventoryHandler().getSlots();
		if (slots > singleSlots) {
			wrapper.changeSize(singleSlots - slots, 0);
		}
		deleteSettingsFromSlot(chest, singleSlots);
		wrapper.getSettingsHandler().getTypeCategory(ItemDisplaySettingsCategory.class).setDisplaySide(DisplaySide.FRONT);
		wrapper.getUpgradeHandler().refreshUpgradeWrappers();
		chest.notifyBlockUpdate();
	}

	/** Both halves show the main one opening: its openers are counted, its block events reach the clients. */
	@Override
	protected void clientTick() {
		prevLidOpenness = lidOpenness;
		boolean open = getMainBlockEntity().isOpen();
		if (open && lidOpenness < 1) {
			lidOpenness = Math.min(1, lidOpenness + 0.1F);
		} else if (!open && lidOpenness > 0) {
			lidOpenness = Math.max(0, lidOpenness - 0.1F);
		}
	}

	public float getOpenness(float partialTicks) {
		return prevLidOpenness + (lidOpenness - prevLidOpenness) * partialTicks;
	}

	@Override
	protected void onOpen() {
		playSound(SoundEvents.BLOCK_CHEST_OPEN, EnumFacing.UP);
	}

	@Override
	protected void onClose() {
		playSound(SoundEvents.BLOCK_CHEST_CLOSE, EnumFacing.UP);
	}

	@Override
	public String getStorageDisplayName() {
		StorageBlockEntity main = getMainBlockEntity();
		return main != this ? main.getStorageDisplayName() : super.getStorageDisplayName();
	}

	@Override
	protected void saveSynchronizedData(NBTTagCompound tag) {
		super.saveSynchronizedData(tag);
		if (doubleMainPos != null) {
			tag.setLong(DOUBLE_MAIN_POS_TAG, doubleMainPos.toLong());
		}
	}

	@Override
	protected void loadSynchronizedData(NBTTagCompound tag) {
		super.loadSynchronizedData(tag);
		doubleMainPos = tag.hasKey(DOUBLE_MAIN_POS_TAG) ? BlockPos.fromLong(tag.getLong(DOUBLE_MAIN_POS_TAG)) : null;
	}

	@Override
	public net.minecraft.util.math.AxisAlignedBB getRenderBoundingBox() {
		return new net.minecraft.util.math.AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 2, 2));
	}
}
