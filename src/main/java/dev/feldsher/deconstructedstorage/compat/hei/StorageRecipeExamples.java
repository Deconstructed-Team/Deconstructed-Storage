package dev.feldsher.deconstructedstorage.compat.hei;

import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.crafting.ShulkerBoxRecipes;
import dev.feldsher.deconstructedstorage.crafting.StorageTierUpgradeRecipe;
import dev.feldsher.deconstructedstorage.init.ModBlocks;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Examples of the recipes that carry over what a storage item has, which HEI can't show from the recipes themselves:
 * one for each storage in the creative tab the recipe takes, with what comes out of it.
 */
public final class StorageRecipeExamples {
	private StorageRecipeExamples() {}

	public static List<IRecipe> getTierUpgradeRecipes() {
		List<IRecipe> examples = new ArrayList<>();
		for (IRecipe recipe : ForgeRegistries.RECIPES) {
			if (recipe instanceof StorageTierUpgradeRecipe.Shaped || recipe instanceof StorageTierUpgradeRecipe.Shapeless) {
				addExamples(examples, recipe);
			}
		}
		return examples;
	}

	public static List<IRecipe> getShulkerBoxFromChestRecipes() {
		List<IRecipe> examples = new ArrayList<>();
		for (IRecipe recipe : ForgeRegistries.RECIPES) {
			if (recipe instanceof ShulkerBoxRecipes.FromChest) {
				addExamples(examples, recipe);
			}
		}
		return examples;
	}

	/** An acacia barrel to a flat one and back. */
	public static List<IRecipe> getFlatBarrelRecipes() {
		List<IRecipe> examples = new ArrayList<>();
		if (ModBlocks.getBarrels().isEmpty()) {
			return examples;
		}
		ItemStack barrel = WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.BARREL), WoodType.ACACIA);
		ItemStack flatBarrel = barrel.copy();
		BarrelBlockItem.setFlatTop(flatBarrel, true);
		examples.add(new ShapelessOreRecipe(null, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(barrel)), flatBarrel));
		examples.add(new ShapelessOreRecipe(null, NonNullList.from(Ingredient.EMPTY, Ingredient.fromStacks(flatBarrel)), barrel));
		return examples;
	}

	private static void addExamples(List<IRecipe> examples, IRecipe recipe) {
		NonNullList<Ingredient> ingredients = recipe.getIngredients();
		for (ItemStack storage : getStorageItems(ingredients)) {
			NonNullList<Ingredient> exampleIngredients = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
			int width = recipe instanceof ShapedOreRecipe ? ((ShapedOreRecipe) recipe).getRecipeWidth() : 3;
			int height = recipe instanceof ShapedOreRecipe ? ((ShapedOreRecipe) recipe).getRecipeHeight() : 3;
			InventoryCrafting grid = new InventoryCrafting(new EmptyContainer(), width, height);
			for (int i = 0; i < ingredients.size(); i++) {
				Ingredient ingredient = ingredients.get(i);
				if (takesItemOf(ingredient, storage)) {
					exampleIngredients.set(i, Ingredient.fromStacks(storage));
					grid.setInventorySlotContents(i, storage.copy());
				} else {
					exampleIngredients.set(i, ingredient);
					ItemStack[] matchingStacks = ingredient.getMatchingStacks();
					if (matchingStacks.length > 0) {
						grid.setInventorySlotContents(i, matchingStacks[0].copy());
					}
				}
			}
			ItemStack result = recipe.getCraftingResult(grid);
			if (recipe instanceof ShapedOreRecipe) {
				CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
				primer.width = width;
				primer.height = height;
				primer.mirrored = false;
				primer.input = exampleIngredients;
				examples.add(new ShapedOreRecipe(null, result, primer));
			} else {
				examples.add(new ShapelessOreRecipe(null, exampleIngredients, result));
			}
		}
	}

	private static boolean takesItemOf(Ingredient ingredient, ItemStack storage) {
		for (ItemStack matchingStack : ingredient.getMatchingStacks()) {
			if (matchingStack.getItem() == storage.getItem()) {
				return true;
			}
		}
		return false;
	}

	/** What the creative tab has of each storage the recipe takes. */
	private static List<ItemStack> getStorageItems(NonNullList<Ingredient> ingredients) {
		NonNullList<ItemStack> storageItems = NonNullList.create();
		Set<Item> storages = new HashSet<>();
		for (Ingredient ingredient : ingredients) {
			for (ItemStack matchingStack : ingredient.getMatchingStacks()) {
				if (matchingStack.getItem() instanceof StorageBlockItem && storages.add(matchingStack.getItem())) {
					matchingStack.getItem().getSubItems(CreativeTabs.SEARCH, storageItems);
				}
			}
		}
		return storageItems;
	}

	private static class EmptyContainer extends Container {
		@Override
		public boolean canInteractWith(EntityPlayer player) {
			return false;
		}
	}
}
