package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedcore.common.gui.ISyncedContainer;
import dev.feldsher.deconstructedcore.network.PacketHandler;
import dev.feldsher.deconstructedcore.network.SyncContainerClientDataMessage;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.DecorationTableBlockEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A decoration table's container: its seven material slots in two columns, trims and cores, its three dye slots, the
 * slot of what's decorated and the result, and the player's inventory. The colors and which slots inherit are set on
 * the client and sent to the server.
 */
public class DecorationTableMenu extends Container implements ISyncedContainer {
	private static final String SET_INHERITANCE_ACTION = "setInheritance";
	public static final String EMPTY_RED_DYE_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_red_dye_slot";
	public static final String EMPTY_GREEN_DYE_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_green_dye_slot";
	public static final String EMPTY_BLUE_DYE_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_blue_dye_slot";
	public static final String EMPTY_MATERIAL_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_material_slot";
	public static final int DECORATION_SLOT_PADDING = 12;
	public static final int FIRST_DYE_SLOT = 7;
	public static final int STORAGE_SLOT = 10;
	public static final int RESULT_SLOT = 11;
	private static final int FIRST_PLAYER_SLOT = 12;

	private final DecorationTableBlockEntity blockEntity;
	private final EntityPlayer player;
	private final Slot resultSlot;
	@Nullable
	private Runnable slotChangedListener = null;

	public DecorationTableMenu(EntityPlayer player, DecorationTableBlockEntity blockEntity) {
		this.player = player;
		this.blockEntity = blockEntity;
		int y = addDecorationSlots();
		Slot storageSlot = addSlotToContainer(new NotifyingSlot(blockEntity.getStorageBlock(), 0, getSlot(FIRST_DYE_SLOT).xPos,
				getSlot(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT).yPos));
		resultSlot = addSlotToContainer(new ResultSlot(storageSlot.xPos + 18 + 18, storageSlot.yPos));
		addPlayerSlots(player.inventory, y + 14);
	}

	public void setSlotChangedListener(@Nullable Runnable listener) {
		slotChangedListener = listener;
	}

	public List<ItemStack> getDecoratedPreviewStacks() {
		return blockEntity.getDecoratedPreviewStacks();
	}

	private int addDecorationSlots() {
		int yOffset = 17;
		ItemStackHandler decorativeBlocks = blockEntity.getDecorativeBlocks();
		int x = 8;
		int y = yOffset;
		y = addDecorationSlot(decorativeBlocks, 0, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 1, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 2, x, y, DECORATION_SLOT_PADDING);
		addDecorationSlot(decorativeBlocks, 3, x, y, DECORATION_SLOT_PADDING);
		y = yOffset + 18 + DECORATION_SLOT_PADDING;
		x += 48;
		y = addDecorationSlot(decorativeBlocks, 4, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 5, x, y, DECORATION_SLOT_PADDING);
		y = addDecorationSlot(decorativeBlocks, 6, x, y, 0);
		x += 44;

		ItemStackHandler dyes = blockEntity.getDyes();
		addDyeSlot(dyes, 0, x, yOffset, EMPTY_RED_DYE_SLOT_BACKGROUND);
		addDyeSlot(dyes, 1, x + 18, yOffset, EMPTY_GREEN_DYE_SLOT_BACKGROUND);
		addDyeSlot(dyes, 2, x + 36, yOffset, EMPTY_BLUE_DYE_SLOT_BACKGROUND);
		return y;
	}

	private void addDyeSlot(ItemStackHandler dyes, int index, int x, int y, String background) {
		Slot slot = new SlotItemHandler(dyes, index, x, y);
		slot.setBackgroundName(background);
		addSlotToContainer(slot);
	}

	private int addDecorationSlot(ItemStackHandler itemHandler, int slotIndex, int x, int y, int yPadding) {
		Slot slot = new NotifyingSlot(itemHandler, slotIndex, x, y);
		slot.setBackgroundName(EMPTY_MATERIAL_SLOT_BACKGROUND);
		addSlotToContainer(slot);
		return y + 18 + yPadding;
	}

	private void addPlayerSlots(InventoryPlayer playerInventory, int y) {
		int playerSlotXOffset = 45;
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, playerSlotXOffset + col * 18, y + row * 18));
			}
		}
		for (int col = 0; col < 9; ++col) {
			addSlotToContainer(new Slot(playerInventory, col, playerSlotXOffset + col * 18, y + 3 * 18 + 4));
		}
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
		Slot slot = getSlot(slotIndex);
		ItemStack slotStack = slot.getStack();
		if (slotStack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack slotStackCopy = slotStack.copy();
		if (slotIndex >= FIRST_PLAYER_SLOT) {
			if (blockEntity.getDecorativeBlocks().isItemValid(0, slotStack)) {
				if (!mergeItemStack(slotStack, 0, FIRST_DYE_SLOT, false)) {
					return ItemStack.EMPTY;
				}
			} else if (isValidDye(slotStack)) {
				if (!mergeItemStack(slotStack, FIRST_DYE_SLOT, STORAGE_SLOT, false)) {
					return ItemStack.EMPTY;
				}
			} else if (blockEntity.getStorageBlock().isItemValid(0, slotStack)) {
				if (!mergeItemStack(slotStack, STORAGE_SLOT, STORAGE_SLOT + 1, false)) {
					return ItemStack.EMPTY;
				}
			} else {
				return ItemStack.EMPTY;
			}
		} else if (!mergeItemStack(slotStack, FIRST_PLAYER_SLOT, inventorySlots.size(), true)) {
			return ItemStack.EMPTY;
		}
		if (slotIndex == RESULT_SLOT) {
			slot.onTake(player, slotStackCopy);
		} else {
			slot.onSlotChanged();
		}
		if (slotStackCopy.getCount() == slotStack.getCount()) {
			return ItemStack.EMPTY;
		}
		blockEntity.updateResultAndMarkDirty();
		return slotStackCopy;
	}

	private boolean isValidDye(ItemStack stack) {
		ItemStackHandler dyes = blockEntity.getDyes();
		for (int slot = 0; slot < dyes.getSlots(); slot++) {
			if (dyes.isItemValid(slot, stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		BlockPos pos = blockEntity.getPos();
		return player.world.getTileEntity(pos) == blockEntity && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}

	public void setSlotMaterialInheritance(int slot, boolean inheritance) {
		blockEntity.setSlotMaterialInheritance(slot, inheritance);
		sendToServer(tag -> {
			tag.setString("action", SET_INHERITANCE_ACTION);
			tag.setInteger("slot", slot);
			tag.setBoolean("inheritance", inheritance);
		});
	}

	public void setMainColor(int color) {
		blockEntity.setMainColor(color);
		sendToServer(tag -> tag.setInteger("mainColor", color));
	}

	public void setAccentColor(int color) {
		blockEntity.setAccentColor(color);
		sendToServer(tag -> tag.setInteger("accentColor", color));
	}

	public boolean isSlotMaterialInherited(int slot) {
		return blockEntity.isSlotMaterialInherited(slot);
	}

	public ItemStack getInheritedItem(int childSlot) {
		return blockEntity.getInheritedItem(childSlot);
	}

	public int getMainColor() {
		return blockEntity.getMainColor();
	}

	public int getAccentColor() {
		return blockEntity.getAccentColor();
	}

	public Slot getResultSlot() {
		return resultSlot;
	}

	private void sendToServer(Consumer<NBTTagCompound> addData) {
		if (!player.world.isRemote) {
			return;
		}
		NBTTagCompound data = new NBTTagCompound();
		addData.accept(data);
		PacketHandler.INSTANCE.sendToServer(new SyncContainerClientDataMessage(data));
	}

	public Map<String, Integer> getPartsNeeded() {
		return blockEntity.getPartsNeeded();
	}

	public Set<String> getMissingDyes() {
		return blockEntity.getMissingDyes();
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		if (data.getString("action").equals(SET_INHERITANCE_ACTION)) {
			setSlotMaterialInheritance(data.getInteger("slot"), data.getBoolean("inheritance"));
		} else if (data.hasKey("mainColor")) {
			setMainColor(data.getInteger("mainColor"));
		} else if (data.hasKey("accentColor")) {
			setAccentColor(data.getInteger("accentColor"));
		}
	}

	public Map<String, Integer> getPartsStored() {
		return blockEntity.getPartsStored();
	}

	/** A slot whose changes the screen hears of: what it shows turning changes with them. */
	private class NotifyingSlot extends SlotItemHandler {
		NotifyingSlot(ItemStackHandler itemHandler, int index, int x, int y) {
			super(itemHandler, index, x, y);
		}

		@Override
		public void onSlotChanged() {
			super.onSlotChanged();
			if (slotChangedListener != null) {
				slotChangedListener.run();
			}
		}
	}

	/** The decorated item, taken only when the dyes it needs are there: it uses the blocks or dyes up, and the item decorated. */
	private class ResultSlot extends Slot {
		ResultSlot(int x, int y) {
			super(new InventoryBasic("result", false, 1), 0, x, y);
		}

		@Override
		public ItemStack getStack() {
			return blockEntity.getResult();
		}

		@Override
		public boolean getHasStack() {
			return !getStack().isEmpty();
		}

		@Override
		public ItemStack decrStackSize(int amount) {
			return blockEntity.extractResult(amount);
		}

		@Override
		public void putStack(ItemStack stack) {
			//noop: only taken
		}

		@Override
		public boolean isItemValid(ItemStack stack) {
			return false;
		}

		@Override
		public boolean canTakeStack(EntityPlayer player) {
			return getHasStack() && getMissingDyes().isEmpty();
		}

		@Override
		public ItemStack onTake(EntityPlayer player, ItemStack stack) {
			if (!player.world.isRemote) {
				blockEntity.consumeIngredientsOnCraft();
				blockEntity.getStorageBlock().extractItem(0, 1, false);
			}
			return super.onTake(player, stack);
		}
	}
}
