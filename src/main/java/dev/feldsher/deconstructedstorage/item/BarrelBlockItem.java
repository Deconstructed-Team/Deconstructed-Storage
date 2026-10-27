package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** A barrel as an item: also whether its top is flat. */
public class BarrelBlockItem extends WoodStorageBlockItem {
	private static final String FLAT_TOP_TAG = "flat_top";

	public BarrelBlockItem(Block block) {
		super(block);
	}

	public static boolean isFlatTop(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return stack.getItem() instanceof BarrelBlockItem && tag != null && tag.getBoolean(FLAT_TOP_TAG);
	}

	public static void setFlatTop(ItemStack stack, boolean flatTop) {
		if (flatTop) {
			StorageNbt.getOrCreateTag(stack).setBoolean(FLAT_TOP_TAG, true);
		} else if (stack.getTagCompound() != null) {
			stack.getTagCompound().removeTag(FLAT_TOP_TAG);
		}
	}
}
