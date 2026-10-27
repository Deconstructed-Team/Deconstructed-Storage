package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestType;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/** A chest stack drawn as the chest it places, closed, as vanilla's chest item. */
public class ChestItemRenderer extends TileEntityItemStackRenderer {
	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		TileEntitySpecialRenderer<ChestBlockEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(ChestBlockEntity.class);
		if (!(renderer instanceof ChestRenderer)) {
			return;
		}
		int mainColor = StorageBlockItem.getMainColor(stack).orElse(-1);
		int accentColor = StorageBlockItem.getAccentColor(stack).orElse(-1);
		WoodType woodType = WoodStorageBlockItem.getWoodType(stack).orElse(mainColor != -1 && accentColor != -1 ? null : WoodType.DEFAULT);
		((ChestRenderer) renderer).render(Block.getBlockFromItem(stack.getItem()), ChestType.SINGLE, EnumFacing.SOUTH, woodType, mainColor, accentColor, 0,
				0, 0, 0, -1);
	}
}
