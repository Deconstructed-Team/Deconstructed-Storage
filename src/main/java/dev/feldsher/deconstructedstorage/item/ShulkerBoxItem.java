package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

/**
 * A shulker box as an item, one to a stack: broken, a box keeps what it holds, which its stack carries, as a packed
 * storage's does.
 */
public class ShulkerBoxItem extends StorageBlockItem {
	private static final String PENDING_ITEMS_TAG = "pending_items";

	public ShulkerBoxItem(Block block) {
		super(block);
		setMaxStackSize(1);
	}

	/** Items to go into the box as it's placed, as a vanilla box's crafted into this one: kept as vanilla keeps them. */
	public static void setPendingItems(ItemStack stack, NBTTagList items) {
		StorageNbt.getOrCreateTag(stack).setTag(PENDING_ITEMS_TAG, items);
	}

	public static NBTTagList getPendingItems(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag == null ? new NBTTagList() : tag.getTagList(PENDING_ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
	}
}
