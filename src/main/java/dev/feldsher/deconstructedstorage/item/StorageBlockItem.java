package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A storage block as an item: it keeps how the storage looks, and puts it back when placed. */
public class StorageBlockItem extends ItemBlock {
	/** Its kinds, woods and colors, are told apart by NBT, not metadata: recipes take any of it by its item alone. */
	public StorageBlockItem(Block block) {
		super(block);
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

	public static void removeMainColor(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(StorageNbt.MAIN_COLOR);
		}
	}

	public static void removeAccentColor(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(StorageNbt.ACCENT_COLOR);
		}
	}

	public static ItemStack setColors(ItemStack stack, int mainColor, int accentColor) {
		NBTTagCompound tag = StorageNbt.getOrCreateTag(stack);
		tag.setInteger(StorageNbt.MAIN_COLOR, mainColor);
		tag.setInteger(StorageNbt.ACCENT_COLOR, accentColor);
		onColorsChanged(stack);
		return stack;
	}

	/**
	 * What it holds, when it's carried as a stack, as a shulker box broken or a packed storage: its contents' UUID, and
	 * how many slots it has. The storage placed from it takes them up again.
	 */
	public static Optional<UUID> getContentsUuid(ItemStack stack) {
		return StorageNbt.getStorageUuid(stack);
	}

	public static void setContents(ItemStack stack, UUID contentsUuid, int numberOfInventorySlots, int numberOfUpgradeSlots) {
		StorageNbt.setStorageUuid(stack, contentsUuid);
		NBTTagCompound tag = StorageNbt.getOrCreateTag(stack);
		tag.setInteger(StorageNbt.NUMBER_OF_INVENTORY_SLOTS, numberOfInventorySlots);
		tag.setInteger(StorageNbt.NUMBER_OF_UPGRADE_SLOTS, numberOfUpgradeSlots);
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

	/** Tinted all over, a storage is of no wood any more. */
	private static void onColorsChanged(ItemStack stack) {
		if (getMainColor(stack).isPresent() && getAccentColor(stack).isPresent()) {
			WoodStorageBlockItem.removeWoodType(stack);
		}
	}
}
