package dev.feldsher.deconstructedstorage.block;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A handler that, within a tick, turns away a stack it already failed to take: pipes offer the same stack to each
 * slot, and the controller puts each into the whole network. Only the stack object itself is remembered.
 */
public class CachedFailedInsertInventoryHandler implements IItemHandlerModifiable {
	private final Supplier<IItemHandlerModifiable> wrappedHandlerGetter;
	private final LongSupplier timeSupplier;
	private long currentCacheTime = 0;
	private final Set<ItemStack> failedInsertStacks = Collections.newSetFromMap(new IdentityHashMap<>());

	public CachedFailedInsertInventoryHandler(Supplier<IItemHandlerModifiable> wrappedHandlerGetter, LongSupplier timeSupplier) {
		this.wrappedHandlerGetter = wrappedHandlerGetter;
		this.timeSupplier = timeSupplier;
	}

	@Override
	public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
		wrappedHandlerGetter.get().setStackInSlot(slot, stack);
	}

	@Override
	public int getSlots() {
		return wrappedHandlerGetter.get().getSlots();
	}

	@Nonnull
	@Override
	public ItemStack getStackInSlot(int slot) {
		return wrappedHandlerGetter.get().getStackInSlot(slot);
	}

	@Nonnull
	@Override
	public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
		if (currentCacheTime != timeSupplier.getAsLong()) {
			failedInsertStacks.clear();
			currentCacheTime = timeSupplier.getAsLong();
		}
		if (failedInsertStacks.contains(stack)) {
			return stack;
		}
		ItemStack result = wrappedHandlerGetter.get().insertItem(slot, stack, simulate);
		if (result == stack) {
			failedInsertStacks.add(stack);
		}
		return result;
	}

	@Nonnull
	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return wrappedHandlerGetter.get().extractItem(slot, amount, simulate);
	}

	@Override
	public int getSlotLimit(int slot) {
		return wrappedHandlerGetter.get().getSlotLimit(slot);
	}
}
