package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.DecorationTableBlock;
import dev.feldsher.deconstructedstorage.block.DecorationTableBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/** The decorated storage lying on the table top, as it would come out; a set up paintbrush isn't shown. */
public class DecorationTableRenderer extends TileEntitySpecialRenderer<DecorationTableBlockEntity> {
	@Override
	public void render(DecorationTableBlockEntity table, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		ItemStack result = table.getResult();
		if (result.isEmpty() || !DecorationTableBlockEntity.consumesIngredients(result) || !table.hasWorld()) {
			return;
		}
		IBlockState state = table.getWorld().getBlockState(table.getPos());
		EnumFacing facing = state.getBlock() instanceof DecorationTableBlock ? state.getValue(DecorationTableBlock.FACING) : EnumFacing.NORTH;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 1.125, z + 0.5);
		GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
		GlStateManager.scale(0.5F, 0.5F, 0.5F);
		RenderHelper.enableStandardItemLighting();
		Minecraft.getMinecraft().getRenderItem().renderItem(result, ItemCameraTransforms.TransformType.FIXED);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popMatrix();
	}
}
