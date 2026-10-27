package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Optional;

/** A storage block as an item: it keeps how the storage looks, and puts it back when placed. */
public class StorageBlockItem extends ItemBlock {
	public StorageBlockItem(Block block) {
		super(block);
		setHasSubtypes(true);
	}

	public static Optional<Integer> getMainColor(ItemStack stack) {
		return getInt(stack, StorageNbt.MAIN_COLOR);
	}

	public static Optional<Integer> getAccentColor(ItemStack stack) {
		return getInt(stack, StorageNbt.ACCENT_COLOR);
	}

	private static Optional<Integer> getInt(ItemStack stack, String key) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasKey(key) ? Optional.of(tag.getInteger(key)) : Optional.empty();
	}

	public static void setMainColor(ItemStack stack, int mainColor) {
		StorageNbt.getOrCreateTag(stack).setInteger(StorageNbt.MAIN_COLOR, mainColor);
		onColorsChanged(stack);
	}

	public static void setAccentColor(ItemStack stack, int accentColor) {
		StorageNbt.getOrCreateTag(stack).setInteger(StorageNbt.ACCENT_COLOR, accentColor);
		onColorsChanged(stack);
	}

	public static ItemStack setColors(ItemStack stack, int mainColor, int accentColor) {
		NBTTagCompound tag = StorageNbt.getOrCreateTag(stack);
		tag.setInteger(StorageNbt.MAIN_COLOR, mainColor);
		tag.setInteger(StorageNbt.ACCENT_COLOR, accentColor);
		onColorsChanged(stack);
		return stack;
	}

	/** Tinted all over, a storage is of no wood any more. */
	private static void onColorsChanged(ItemStack stack) {
		if (getMainColor(stack).isPresent() && getAccentColor(stack).isPresent()) {
			WoodStorageBlockItem.removeWoodType(stack);
		}
	}
}
