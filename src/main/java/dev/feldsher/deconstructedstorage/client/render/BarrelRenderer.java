package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * The items a barrel shows on its front, up to four, and its upgrades: its model has the rest. The front of one with a
 * rim around its lid is a pixel in; the upgrades sit on the rim.
 */
public class BarrelRenderer extends TileEntitySpecialRenderer<BarrelBlockEntity> {
	private static final EnumFacing[] SIDES = {EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, null};

	/**
	 * A hidden tier or lock, see-through, while the player holds what shows it: the barrel model's quads of them, a hair
	 * bigger than the barrel, as 1.21 draws them.
	 */
	public static void renderHiddenTierAndLock(StorageBlockEntity barrel, double x, double y, double z) {
		boolean hiddenTier = !barrel.shouldShowTier() && HeldItemDisplays.holdsItemThatShowsHiddenTiers();
		boolean hiddenLock = barrel.isLocked() && !barrel.shouldShowLock() && HeldItemDisplays.holdsToolInLockMode();
		if (!hiddenTier && !hiddenLock) {
			return;
		}
		World world = barrel.getWorld();
		BlockPos pos = barrel.getPos();
		IBlockState state = world.getBlockState(pos).getActualState(world, pos);
		IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
		if (!(model instanceof BarrelBakedModel)) {
			return;
		}
		BarrelBakedModel barrelModel = (BarrelBakedModel) model;
		List<BakedQuad> quads = new ArrayList<>();
		for (EnumFacing side : SIDES) {
			if (hiddenTier) {
				quads.addAll(barrelModel.getTierQuads(side));
			}
			if (hiddenLock) {
				quads.addAll(barrelModel.getLockQuads(side));
			}
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		GlStateManager.scale(1.01F, 1.01F, 1.01F);
		GlStateManager.translate(-0.5, -0.5, -0.5);
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		// the barrel's own block has no light in it: its faces are lit by what's around them
		int light = 0;
		for (EnumFacing side : EnumFacing.values()) {
			light = Math.max(light, world.getCombinedLight(pos.offset(side), 0));
		}
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536F);
		// lit only by the lightmap, as the chunk draws the barrel: the item lighting the dispatcher leaves on darkens it
		GlStateManager.disableLighting();
		StorageFrontRenderer.beginSeeThrough();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
		for (BakedQuad quad : quads) {
			LightUtil.renderQuadColor(buffer, quad, 0x80_FFFFFF);
		}
		tessellator.draw();
		StorageFrontRenderer.endSeeThrough();
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}

	@Override
	public void render(BarrelBlockEntity barrel, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!barrel.hasWorld() || barrel.isPacked() || destroyStage >= 0) {
			return;
		}
		renderHiddenTierAndLock(barrel, x, y, z);
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
