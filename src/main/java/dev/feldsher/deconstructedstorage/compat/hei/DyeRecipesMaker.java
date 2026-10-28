package dev.feldsher.deconstructedstorage.compat.hei;

import dev.feldsher.deconstructedcore.util.ColorHelper;
import dev.feldsher.deconstructedstorage.block.WoodStorageBlockBase;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.init.ModBlocks;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Examples of the storage dye recipe for HEI to show, which it can't from the recipe itself, as it takes any dyes in
 * any places: for each storage, one per color under it, and one with yellow left of it for the core and lime right of
 * it for the trim. A wooden storage goes in of any wood.
 */
public final class DyeRecipesMaker {
	private DyeRecipesMaker() {}

	public static List<IRecipe> getRecipes() {
		List<IRecipe> recipes = new ArrayList<>();
		for (Block block : ModBlocks.getAllBlocks()) {
			Ingredient storage = Ingredient.fromStacks(getStorageStacks(block));
			for (EnumDyeColor color : EnumDyeColor.values()) {
				ItemStack result = StorageBlockItem.setColors(new ItemStack(block), getDyedColor(color), getDyedColor(color));
				recipes.add(new ShapedOreRecipe(null, result, "S", "D", 'S', storage, 'D', ColorHelper.getDyeOreName(color)));
			}
			ItemStack result = StorageBlockItem.setColors(new ItemStack(block), getDyedColor(EnumDyeColor.YELLOW), getDyedColor(EnumDyeColor.LIME));
			recipes.add(new ShapedOreRecipe(null, result, "YSL",
					'Y', ColorHelper.getDyeOreName(EnumDyeColor.YELLOW), 'S', storage, 'L', ColorHelper.getDyeOreName(EnumDyeColor.LIME)));
		}
		return recipes;
	}

	/** As the recipe colors an undyed storage. */
	private static int getDyedColor(EnumDyeColor color) {
		return ColorHelper.calculateColor(0, 0, Collections.singletonList(color)) & 0xFFFFFF;
	}

	private static ItemStack[] getStorageStacks(Block block) {
		if (!(block instanceof WoodStorageBlockBase)) {
			return new ItemStack[] {new ItemStack(block)};
		}
		ItemStack[] stacks = new ItemStack[WoodType.values().length];
		for (WoodType woodType : WoodType.values()) {
			stacks[woodType.ordinal()] = WoodStorageBlockItem.setWoodType(new ItemStack(block), woodType);
		}
		return stacks;
	}
}
