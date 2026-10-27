package dev.feldsher.deconstructedstorage.upgrades.hopper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.ItemStackKey;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogic;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** A filter whose contents option goes by what the inventory items are pushed into holds, not the storage. */
public class TargetContentsFilterLogic extends ContentsFilterLogic {
	private Set<ItemStackKey> inventoryFilterStacks = new HashSet<>();
	private final LoadingCache<IItemHandler, Set<ItemStackKey>> inventoryCache = CacheBuilder.newBuilder().weakKeys().expireAfterWrite(5, TimeUnit.SECONDS)
			.build(new CacheLoader<IItemHandler, Set<ItemStackKey>>() {
				@Override
				public Set<ItemStackKey> load(IItemHandler inventory) {
					return InventoryHelper.getUniqueStacks(inventory);
				}
			});

	public TargetContentsFilterLogic(ItemStack upgrade, Consumer<ItemStack> saveHandler, int filterSlotCount, Supplier<InventoryHandler> getInventoryHandler, String attributesKey) {
		super(upgrade, saveHandler, filterSlotCount, getInventoryHandler, attributesKey);
	}

	public void setInventory(IItemHandler inventory) {
		inventoryFilterStacks = inventoryCache.getUnchecked(inventory);
	}

	@Override
	public boolean matchesFilter(ItemStack stack) {
		if (!shouldFilterByStorage()) {
			return super.matchesFilter(stack);
		}

		for (ItemStackKey filterStack : inventoryFilterStacks) {
			if (stackMatchesFilter(stack, filterStack.getStack())) {
				return true;
			}
		}
		return false;
	}
}
