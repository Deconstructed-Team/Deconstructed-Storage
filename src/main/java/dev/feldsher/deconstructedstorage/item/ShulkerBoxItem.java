package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A shulker box as an item. Broken, a box keeps what it holds: the stack carries its contents' UUID, and how many slots
 * it has, and the box placed from it takes them up again.
 */
public class ShulkerBoxItem extends StorageBlockItem {
	private static final String PENDING_ITEMS_TAG = "pending_items";

	public ShulkerBoxItem(Block block) {
		super(block);
		setMaxStackSize(1);
	}

	public static Optional<UUID> getContentsUuid(ItemStack stack) {
		return StorageNbt.getStorageUuid(stack);
	}

	public static void setContents(ItemStack stack, UUID contentsUuid, int numberOfInventorySlots, int numberOfUpgradeSlots) {
		StorageNbt.setStorageUuid(stack, contentsUuid);
		NBTTagCompound tag = StorageNbt.getOrCreateTag(stack);
		tag.setInteger(StorageNbt.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		tag.setInteger(StorageNbt.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
	}

	/** Items to go into the box as it's placed, as a vanilla box's crafted into this one: kept as vanilla keeps them. */
	public static void setPendingItems(ItemStack stack, NBTTagList items) {
		StorageNbt.getOrCreateTag(stack).setTag(PENDING_ITEMS_TAG, items);
	}

	public static NBTTagList getPendingItems(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag == null ? new NBTTagList() : tag.getTagList(PENDING_ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
	}

	public static int getNumberOfInventorySlots(ItemStack stack, int defaultValue) {
		return StorageNbt.getInt(stack, StorageNbt.NUMBER_OF_INVENTORY_SLOTS, defaultValue);
	}

	public static int getNumberOfUpgradeSlots(ItemStack stack, int defaultValue) {
		return StorageNbt.getInt(stack, StorageNbt.NUMBER_OF_UPGRADE_SLOTS, defaultValue);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);
		if (flag.isAdvanced()) {
			getContentsUuid(stack).ifPresent(uuid -> tooltip.add(TextFormatting.DARK_GRAY + "UUID: " + uuid));
		}
	}
}
