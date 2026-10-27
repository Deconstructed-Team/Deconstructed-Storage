package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * A locked storage as pipes see it: it takes in only items it already holds, or that its memorized slots are set for.
 * The rest it hands out and reports as its inventory does.
 */
class LockedItemHandler implements IItemHandler {
	private final Supplier<IItemHandler> inputOutput;
	private final Supplier<InventoryHandler> inventory;
	private final Supplier<MemorySettingsCategory> memorySettings;

	LockedItemHandler(Supplier<IItemHandler> inputOutput, Supplier<InventoryHandler> inventory, Supplier<MemorySettingsCategory> memorySettings) {
		this.inputOutput = inputOutput;
		this.inventory = inventory;
		this.memorySettings = memorySettings;
	}

	@Override
	public int getSlots() {
		return inputOutput.get().getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return inputOutput.get().getStackInSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (stack.isEmpty() || !isAllowed(stack)) {
			return stack;
		}
		return inputOutput.get().insertItem(slot, stack, simulate);
	}

	private boolean isAllowed(ItemStack stack) {
		if (memorySettings.get().matchesFilter(stack)) {
			return true;
		}
		InventoryHandler handler = inventory.get();
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (ItemHandlerHelper.canItemStacksStack(handler.getStackInSlot(slot), stack)) {
				return true;
			}
		}
		return false;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return inputOutput.get().extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return inputOutput.get().getSlotLimit(slot);
	}
}
