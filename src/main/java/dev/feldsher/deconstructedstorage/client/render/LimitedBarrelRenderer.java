package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedcore.util.CountAbbreviator;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlock;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * A limited barrel's front: each slot's item where the slot is, its count under it, its fill level by its side, and the
 * upgrades. Its model has the rest.
 */
public class LimitedBarrelRenderer extends TileEntitySpecialRenderer<LimitedBarrelBlockEntity> {
	private static final float MULTIPLE_ITEMS_FONT_SCALE = 1 / 96F;
	private static final float SINGLE_ITEM_FONT_SCALE = 1 / 48F;
	private static final String INFINITE_COUNT = "∞";

	@Override
	public void render(LimitedBarrelBlockEntity barrel, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (!barrel.hasWorld() || barrel.isPacked() || destroyStage >= 0) {
			return;
		}
		BarrelRenderer.renderHiddenTierAndLock(barrel, x, y, z);
		IBlockState state = barrel.getWorld().getBlockState(barrel.getPos());
		if (!(state.getBlock() instanceof LimitedBarrelBlock)) {
			return;
		}
		LimitedBarrelBlock block = (LimitedBarrelBlock) state.getBlock();
		int slots = block.getNumberOfInventorySlots();
		RenderInfo renderInfo = barrel.getStorageWrapper().getRenderInfo();
		boolean flatTop = barrel.isFlatTop();
		EnumFacing front = block.getFacing(state);

		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		StorageFrontRenderer.rotateToFront(front, LimitedBarrelBlock.getFrontUp(state));
		GlStateManager.translate(-0.5, -0.5, -(0.5 - (flatTop ? 0 : 1 / 16D)));
		StorageFrontRenderer.setFrontLight(barrel.getWorld(), barrel.getPos(), front);

		RenderInfo.ItemDisplayRenderInfo itemDisplay = renderInfo.getItemDisplayRenderInfo();
		for (RenderInfo.DisplayItem displayItem : itemDisplay.getDisplayItems()) {
			if (displayItem.getSlotIndex() < slots) {
				StorageFrontRenderer.renderItem(displayItem.getItem(), displayItem.getRotation(), displayItem.getSlotIndex(), slots, 0.5F);
			}
		}
		StorageFrontRenderer.renderInaccessibleSlots(itemDisplay.getInaccessibleSlots(), slots, 0.5F);

		boolean holdsItemThatShowsUpgrades = HeldItemDisplays.holdsItemThatShowsUpgrades();
		if (barrel.shouldShowUpgrades() || holdsItemThatShowsUpgrades) {
			StorageFrontRenderer.renderUpgradeItems(renderInfo.getUpgradeItems(), 0, 0, flatTop ? 0 : -1 / 16F, holdsItemThatShowsUpgrades,
					HeldItemDisplays.showsDisabledUpgrades(barrel));
		}
		if (barrel.shouldShowFillLevels() || HeldItemDisplays.holdsItemThatShowsFillLevels()) {
			renderFillLevels(itemDisplay.getSlotFillRatios(), !barrel.shouldShowFillLevels() && HeldItemDisplays.holdsToolInFillLevelDisplayMode());
		}
		if (barrel.shouldShowCounts()) {
			renderCounts(barrel, itemDisplay, flatTop);
		}
		GlStateManager.popMatrix();
	}

	/** Each count under its slot's item, in the slot's color; an infinite slot's as the infinity sign. */
	private void renderCounts(LimitedBarrelBlockEntity barrel, RenderInfo.ItemDisplayRenderInfo itemDisplay, boolean flatTop) {
		List<Integer> slotCounts = itemDisplay.getSlotCounts();
		List<Integer> infiniteSlots = itemDisplay.getInfiniteSlots();
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;
		float countDisplayYOffset = -(slotCounts.size() == 1 ? 0.25F : 0.11F);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		for (int slot = 0; slot < slotCounts.size(); slot++) {
			int count = slotCounts.get(slot);
			if (count <= 0) {
				continue;
			}
			boolean infinite = infiniteSlots.contains(slot);
			float[] frontOffset = StorageFrontRenderer.getFrontOffset(slot, slotCounts.size(), 0.5F);
			float scale = slotCounts.size() == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE;
			if (infinite) {
				scale *= 2;
			}
			String countString = infinite ? INFINITE_COUNT : CountAbbreviator.abbreviate(count, slotCounts.size() == 1 ? 6 : 5);
			GlStateManager.pushMatrix();
			// a quarter of a pixel out from the front, which with a rim around it is a pixel in
			GlStateManager.translate(frontOffset[0], frontOffset[1] + (infinite ? countDisplayYOffset / 1.8F : countDisplayYOffset), flatTop ? -0.001 : -0.25 / 16D);
			// as the item, facing out of the front; text is drawn downward
			GlStateManager.rotate(180, 0, 1, 0);
			GlStateManager.scale(scale, -scale, scale);
			if (infinite) {
				font.drawString(countString, -font.getStringWidth(countString) / 2, 0, barrel.getSlotColor(slot));
			} else {
				drawBold(font, countString, barrel.getSlotColor(slot));
			}
			GlStateManager.popMatrix();
		}
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
		GlStateManager.color(1, 1, 1, 1);
	}

	/**
	 * Bold, centered on x 0: each char twice, the second 0.99 to the right. 1.12's bold puts it a whole pixel over, but a
	 * char is drawn 0.01 short of its width, so up close a seam shows between the two.
	 */
	private static void drawBold(FontRenderer font, String text, int color) {
		float x = -(font.getStringWidth(text) + text.length()) / 2F;
		for (char c : text.toCharArray()) {
			String ch = String.valueOf(c);
			font.drawString(ch, x, 0, color, false);
			font.drawString(ch, x + 0.99F, 0, color, false);
			x += font.getCharWidth(c) + 1;
		}
	}

	/** A bar by each slot, filled as far as the slot is; see-through while the tool is set to show the hidden ones. */
	private void renderFillLevels(List<Float> slotFillLevels, boolean translucent) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sprite = minecraft.getTextureMapBlocks().getAtlasSprite(BarrelParts.FILL_INDICATORS);
		GlStateManager.disableLighting();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		float alpha = translucent ? 0.5F : 1;
		switch (slotFillLevels.size()) {
			case 1:
				renderFillLevel(sprite, slotFillLevels.get(0), 1 / 16F, 1 / 16F, true, alpha);
				break;
			case 2:
				renderFillLevel(sprite, slotFillLevels.get(0), 1 / 16F, 9 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(1), 1 / 16F, 1 / 16F, false, alpha);
				break;
			case 3:
				renderFillLevel(sprite, slotFillLevels.get(0), 1 / 16F, 9 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(1), 14 / 16F, 1 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(2), 1 / 16F, 1 / 16F, false, alpha);
				break;
			case 4:
				renderFillLevel(sprite, slotFillLevels.get(0), 14 / 16F, 9 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(1), 1 / 16F, 9 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(2), 14 / 16F, 1 / 16F, false, alpha);
				renderFillLevel(sprite, slotFillLevels.get(3), 1 / 16F, 1 / 16F, false, alpha);
				break;
			default:
				break;
		}
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.enableLighting();
	}

	/** Three fifths of a pixel wide, as tall as the slot's side less a fifth of a pixel at each end, from the bottom up. */
	private void renderFillLevel(TextureAtlasSprite sprite, float fillLevel, float x, float y, boolean large, float alpha) {
		if (fillLevel <= 0) {
			return;
		}
		int barHeight = large ? 14 : 6;
		float x1 = x + 1 / 16F / 5F;
		float y1 = y + 1 / 16F / 5F;
		float x2 = x1 + 1 / 16F / 5F * 3;
		float y2 = y1 + fillLevel * 1 / 16F / 5F * (barHeight * 5 - 2);
		float minU = large ? 0 : 3 / 128F;
		float maxV = large ? 68 / 128F : 28 / 128F;
		float u1 = sprite.getInterpolatedU(minU * 16);
		float u2 = sprite.getInterpolatedU((minU + 3 / 128F) * 16);
		float v1 = sprite.getInterpolatedV((1 - fillLevel) * maxV * 16);
		float v2 = sprite.getInterpolatedV(maxV * 16);
		float z = -0.001F;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
		buffer.pos(x1, y1, z).tex(u1, v2).color(1, 1, 1, alpha).endVertex();
		buffer.pos(x2, y1, z).tex(u2, v2).color(1, 1, 1, alpha).endVertex();
		buffer.pos(x2, y2, z).tex(u2, v1).color(1, 1, 1, alpha).endVertex();
		buffer.pos(x1, y2, z).tex(u1, v1).color(1, 1, 1, alpha).endVertex();
		tessellator.draw();
	}
}
