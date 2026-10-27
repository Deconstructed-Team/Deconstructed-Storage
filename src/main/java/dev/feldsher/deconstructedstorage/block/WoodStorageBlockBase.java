package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import java.util.function.IntSupplier;

/** A storage made of wood: of each of the woods, or tinted, as its stack says. */
public abstract class WoodStorageBlockBase extends StorageBlockBase {
	protected WoodStorageBlockBase(IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier) {
		super(Material.WOOD, numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
	}

	@Override
	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		if (be instanceof WoodStorageBlockEntity) {
			WoodStorageBlockEntity wbe = (WoodStorageBlockEntity) be;
			StorageWrapper storageWrapper = wbe.getStorageWrapper();
			if (storageWrapper.hasMainColor()) {
				StorageBlockItem.setMainColor(stack, storageWrapper.getMainColor());
			}
			if (storageWrapper.hasAccentColor()) {
				StorageBlockItem.setAccentColor(stack, storageWrapper.getAccentColor());
			}
			wbe.getWoodType().ifPresent(woodType -> WoodStorageBlockItem.setWoodType(stack, woodType));
		}
	}

	@Override
	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		super.setPlacedData(stack, be);
		if (be instanceof WoodStorageBlockEntity) {
			WoodStorageBlockEntity wbe = (WoodStorageBlockEntity) be;
			wbe.getStorageWrapper().setColors(StorageBlockItem.getMainColor(stack).orElse(-1), StorageBlockItem.getAccentColor(stack).orElse(-1));
			wbe.setWoodType(WoodStorageBlockItem.getWoodType(stack).orElse(null));
		}
	}

	/** Each wood, then, for the basic tier, a tinted one of each dye color, and one yellow and lime. */
	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
		for (WoodType woodType : WoodType.values()) {
			items.add(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType));
		}
		if (isBasicTier()) {
			for (EnumDyeColor color : EnumDyeColor.values()) {
				items.add(StorageBlockItem.setColors(new ItemStack(this), color.getColorValue(), color.getColorValue()));
			}
			items.add(StorageBlockItem.setColors(new ItemStack(this), EnumDyeColor.YELLOW.getColorValue(), EnumDyeColor.LIME.getColorValue()));
		}
	}

	protected abstract boolean isBasicTier();
}
