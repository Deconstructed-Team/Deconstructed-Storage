package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedcore.crafting.StorageDyeRecipeBase;
import dev.feldsher.deconstructedcore.util.ColorHelper;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.List;

/** Dyes a storage item: its core by the dyes left of it and in its column, its trim by those right of it and in its column. */
public class StorageDyeRecipe extends StorageDyeRecipeBase {
	@Override
	protected boolean isDyeableStorageItem(ItemStack stack) {
		return stack.getItem() instanceof StorageBlockItem;
	}

	@Override
	protected void applyColors(ItemStack coloredStorage, List<EnumDyeColor> mainDyes, List<EnumDyeColor> trimDyes) {
		// without the alpha calculateColor puts in: storages keep their colors as plain RGB, as the creative tab's are
		if (!mainDyes.isEmpty()) {
			StorageBlockItem.setMainColor(coloredStorage, ColorHelper.calculateColor(StorageBlockItem.getMainColor(coloredStorage).orElse(0), 0, mainDyes) & 0xFFFFFF);
		}
		if (!trimDyes.isEmpty()) {
			StorageBlockItem.setAccentColor(coloredStorage, ColorHelper.calculateColor(StorageBlockItem.getAccentColor(coloredStorage).orElse(0), 0, trimDyes) & 0xFFFFFF);
		}
	}

	/** {@code "type": "deconstructedstorage:storage_dye"}, nothing else. */
	public static class Factory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			return new StorageDyeRecipe();
		}
	}
}
