package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.WoodType;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nullable;
import java.util.Optional;

/** A wooden storage as an item, named after its wood: "Spruce Barrel", or just "Barrel" when it's tinted. */
public class WoodStorageBlockItem extends StorageBlockItem {
	private static final String WOOD_TYPE_TAG = "wood_type";

	public WoodStorageBlockItem(Block block) {
		super(block);
	}

	public static Optional<WoodType> getWoodType(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.hasKey(WOOD_TYPE_TAG) ? Optional.ofNullable(WoodType.fromName(tag.getString(WOOD_TYPE_TAG))) : Optional.empty();
	}

	public static ItemStack setWoodType(ItemStack stack, WoodType woodType) {
		StorageNbt.getOrCreateTag(stack).setString(WOOD_TYPE_TAG, woodType.getName());
		return stack;
	}

	static void removeWoodType(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(WOOD_TYPE_TAG);
		}
	}

	@SuppressWarnings("deprecation") // the server's translation, as ItemBlock names its stacks
	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		return getDisplayName(getTranslationKey(stack) + ".name", getWoodType(stack).orElse(null));
	}

	@SuppressWarnings("deprecation")
	public static String getDisplayName(String translationKey, @Nullable WoodType woodType) {
		if (woodType == null) {
			return I18n.translateToLocalFormatted(translationKey, "", "").trim();
		}
		return I18n.translateToLocalFormatted(translationKey, I18n.translateToLocal("wood_name." + DeconstructedStorage.MOD_ID + "." + woodType.getName()), " ");
	}
}
