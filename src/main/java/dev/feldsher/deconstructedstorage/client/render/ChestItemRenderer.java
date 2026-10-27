package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestType;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlock;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlockEntity;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/** A chest or shulker box stack drawn as the block it places, closed, as vanilla's chest and shulker box items. */
public class ChestItemRenderer extends TileEntityItemStackRenderer {
	@Override
	public void renderByItem(ItemStack stack, float partialTicks) {
		int mainColor = StorageBlockItem.getMainColor(stack).orElse(-1);
		int accentColor = StorageBlockItem.getAccentColor(stack).orElse(-1);
		Block block = Block.getBlockFromItem(stack.getItem());
		if (block instanceof ShulkerBoxBlock) {
			TileEntitySpecialRenderer<ShulkerBoxBlockEntity> shulkerRenderer = TileEntityRendererDispatcher.instance.getRenderer(ShulkerBoxBlockEntity.class);
			if (shulkerRenderer instanceof ShulkerBoxRenderer) {
				((ShulkerBoxRenderer) shulkerRenderer).render(block, EnumFacing.UP, mainColor, accentColor, true, 0, 0, 0, 0, -1);
			}
			return;
		}
		TileEntitySpecialRenderer<ChestBlockEntity> renderer = TileEntityRendererDispatcher.instance.getRenderer(ChestBlockEntity.class);
		if (!(renderer instanceof ChestRenderer)) {
			return;
		}
		WoodType woodType = WoodStorageBlockItem.getWoodType(stack).orElse(mainColor != -1 && accentColor != -1 ? null : WoodType.DEFAULT);
		((ChestRenderer) renderer).render(block, ChestType.SINGLE, EnumFacing.SOUTH, woodType, mainColor, accentColor, StorageBlockItem.getContentsUuid(stack).isPresent(), true, 0,
				0, 0, 0, -1);
	}
}
