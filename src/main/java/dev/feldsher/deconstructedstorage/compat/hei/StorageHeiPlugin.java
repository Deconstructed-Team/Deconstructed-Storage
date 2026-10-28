package dev.feldsher.deconstructedstorage.compat.hei;

import dev.feldsher.deconstructedcore.compat.hei.CraftingRecipeTransferHandler;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlock;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.crafting.ShulkerBoxRecipes;
import dev.feldsher.deconstructedstorage.crafting.StorageTierUpgradeRecipe;
import dev.feldsher.deconstructedstorage.init.ModBlocks;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.ChestBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Storages in HEI: told apart by wood, colors, flat top and double chest; the recipes that keep what the storage had
 * (dyeing, tier upgrades, shulker boxes from chests, flattening the top) shown by an example for each storage there
 * is in the creative tab, with those recipes themselves hidden, as they show only an unset storage; the upgrades that
 * work as a crafting table or a stonecutter listed with those recipes. Found by HEI's annotation scan, so nothing
 * loads it without HEI.
 */
@JEIPlugin
public class StorageHeiPlugin implements IModPlugin {
	private static final String FUTURE_MC_STONECUTTER = "container.jei.futuremc.stonecutter.name";

	@Override
	public void registerItemSubtypes(ISubtypeRegistry subtypeRegistry) {
		for (Block block : ModBlocks.getAllBlocks()) {
			subtypeRegistry.registerSubtypeInterpreter(Item.getItemFromBlock(block), StorageHeiPlugin::getSubtypeInfo);
		}
	}

	private static String getSubtypeInfo(ItemStack stack) {
		Block block = Block.getBlockFromItem(stack.getItem());
		StringBuilder info = new StringBuilder();
		if (!(block instanceof ShulkerBoxBlock)) {
			info.append("woodName:").append(WoodStorageBlockItem.getWoodType(stack).map(WoodType::getName).orElse("")).append(',');
		}
		info.append("mainColor:").append(StorageBlockItem.getMainColor(stack).map(String::valueOf).orElse(""));
		info.append(",accentColor:").append(StorageBlockItem.getAccentColor(stack).map(String::valueOf).orElse(""));
		if (block instanceof BarrelBlock) {
			info.append(",flatTop:").append(BarrelBlockItem.isFlatTop(stack));
		} else if (block instanceof ChestBlock) {
			info.append(",doubleChest:").append(ChestBlockItem.isDoubleChest(stack));
		}
		return info.toString();
	}

	@Override
	public void register(IModRegistry registry) {
		registry.addRecipes(DyeRecipesMaker.getRecipes(), VanillaRecipeCategoryUid.CRAFTING);
		registry.addRecipes(StorageRecipeExamples.getTierUpgradeRecipes(), VanillaRecipeCategoryUid.CRAFTING);
		registry.addRecipes(StorageRecipeExamples.getShulkerBoxFromChestRecipes(), VanillaRecipeCategoryUid.CRAFTING);
		registry.addRecipes(StorageRecipeExamples.getFlatBarrelRecipes(), VanillaRecipeCategoryUid.CRAFTING);

		registry.addRecipeCatalyst(new ItemStack(ModItems.CRAFTING_UPGRADE), VanillaRecipeCategoryUid.CRAFTING);
		registry.getRecipeTransferRegistry().addRecipeTransferHandler(
				new CraftingRecipeTransferHandler<>(StorageContainerMenu.class, registry.getJeiHelpers().recipeTransferHandlerHelper()), VanillaRecipeCategoryUid.CRAFTING);
		// Future MC's category, there only with it
		if (Loader.isModLoaded("futuremc")) {
			registry.addRecipeCatalyst(new ItemStack(ModItems.STONECUTTER_UPGRADE), FUTURE_MC_STONECUTTER);
		}
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime runtime) {
		IRecipeRegistry recipeRegistry = runtime.getRecipeRegistry();
		for (IRecipe recipe : getRecipesShownByExamples()) {
			IRecipeWrapper wrapper = recipeRegistry.getRecipeWrapper(recipe, VanillaRecipeCategoryUid.CRAFTING);
			if (wrapper != null) {
				recipeRegistry.hideRecipe(wrapper, VanillaRecipeCategoryUid.CRAFTING);
			}
		}
	}

	private static List<IRecipe> getRecipesShownByExamples() {
		List<IRecipe> recipes = new ArrayList<>();
		for (IRecipe recipe : ForgeRegistries.RECIPES) {
			if (recipe instanceof StorageTierUpgradeRecipe.Shaped || recipe instanceof StorageTierUpgradeRecipe.Shapeless || recipe instanceof ShulkerBoxRecipes.FromChest) {
				recipes.add(recipe);
			}
		}
		return recipes;
	}
}
