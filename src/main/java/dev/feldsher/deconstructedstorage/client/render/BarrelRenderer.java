package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

/**
 * The items a barrel shows on its front, up to four, and its upgrades: its model has the rest. The front of one with a
 * rim around its lid is a pixel in; the upgrades sit on the rim.
 */
public class BarrelRenderer extends TileEntitySpecialRenderer<BarrelBlockEntity> {
	@Override
	public void render(BarrelBlockEntity barrel, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!barrel.hasWorld() || barrel.isPacked() || destroyStage >= 0) {
			return;
		}
		RenderInfo renderInfo = barrel.getStorageWrapper().getRenderInfo();
		boolean holdsItemThatShowsUpgrades = HeldItemDisplays.holdsItemThatShowsUpgrades();
		boolean showsUpgrades = barrel.shouldShowUpgrades() || holdsItemThatShowsUpgrades;
		RenderInfo.ItemDisplayRenderInfo itemDisplay = renderInfo.getItemDisplayRenderInfo();
		if (itemDisplay.getDisplayItems().isEmpty() && itemDisplay.getInaccessibleSlots().isEmpty() && !showsUpgrades) {
			return;
		}
		IBlockState state = barrel.getWorld().getBlockState(barrel.getPos());
		if (!(state.getBlock() instanceof BarrelBlock)) {
			return;
		}
		EnumFacing facing = state.getValue(BarrelBlock.FACING);
		boolean flatTop = state.getValue(BarrelBlock.FLAT_TOP);
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		StorageFrontRenderer.rotateToFront(facing);
		GlStateManager.translate(-0.5, -0.5, -(0.5 - (flatTop ? 0 : 1 / 16D)));
		StorageFrontRenderer.setFrontLight(barrel.getWorld(), barrel.getPos(), facing);
		StorageFrontRenderer.renderDisplayItems(renderInfo, 0.5, 4);
		StorageFrontRenderer.renderInaccessibleSlots(itemDisplay.getInaccessibleSlots(), Math.min(itemDisplay.getDisplayItems().size(), 4), 0.5F);
		if (showsUpgrades) {
			StorageFrontRenderer.renderUpgradeItems(renderInfo.getUpgradeItems(), 0, 0, flatTop ? 0 : -1 / 16F, holdsItemThatShowsUpgrades,
					HeldItemDisplays.showsDisabledUpgrades(barrel));
		}
		GlStateManager.popMatrix();
	}
}
