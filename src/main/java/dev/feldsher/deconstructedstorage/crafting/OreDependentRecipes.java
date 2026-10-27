package dev.feldsher.deconstructedstorage.crafting;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreIngredient;
import net.minecraftforge.registries.IForgeRegistryModifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipes made of metals vanilla 1.12 doesn't have, as copper and netherite, which come from other mods through the ore
 * dictionary. In a pack where no mod adds one, the recipes that need it are taken out once every mod has registered
 * its ores, and the items are left for the pack to give recipes of its own.
 */
public final class OreDependentRecipes {
	private OreDependentRecipes() {}

	public static void removeRecipesWithoutOres() {
		IForgeRegistryModifiable<IRecipe> recipes = (IForgeRegistryModifiable<IRecipe>) ForgeRegistries.RECIPES;
		List<ResourceLocation> toRemove = new ArrayList<>();
		for (IRecipe recipe : recipes.getValuesCollection()) {
			ResourceLocation name = recipe.getRegistryName();
			if (name == null || !name.getNamespace().equals(DeconstructedStorage.MOD_ID)) {
				continue;
			}
			for (Ingredient ingredient : recipe.getIngredients()) {
				if (ingredient instanceof OreIngredient && ingredient.getMatchingStacks().length == 0) {
					toRemove.add(name);
					break;
				}
			}
		}
		for (ResourceLocation name : toRemove) {
			recipes.remove(name);
			DeconstructedStorage.LOGGER.warn("No mod adds an ore {} needs: it's left without a recipe", name);
		}
	}
}
