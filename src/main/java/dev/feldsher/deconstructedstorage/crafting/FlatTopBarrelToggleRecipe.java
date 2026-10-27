package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.registries.IForgeRegistryEntry;

/** A barrel alone in the grid comes out with its top flat, or a flat one with its top back. */
public class FlatTopBarrelToggleRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		return !getBarrel(inv).isEmpty();
	}

	private static ItemStack getBarrel(InventoryCrafting inv) {
		ItemStack barrel = ItemStack.EMPTY;
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (slotStack.isEmpty()) {
				continue;
			}
			if (!barrel.isEmpty() || !(slotStack.getItem() instanceof BarrelBlockItem)) {
				return ItemStack.EMPTY;
			}
			barrel = slotStack;
		}
		return barrel;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		ItemStack barrel = getBarrel(inv);
		if (barrel.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack result = barrel.copy();
		result.setCount(1);
		BarrelBlockItem.setFlatTop(result, !BarrelBlockItem.isFlatTop(barrel));
		return result;
	}

	@Override
	public boolean canFit(int width, int height) {
		return width * height >= 1;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	/** {@code "type": "deconstructedstorage:flat_top_barrel_toggle"}, nothing else. */
	public static class Factory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			return new FlatTopBarrelToggleRecipe();
		}
	}
}
