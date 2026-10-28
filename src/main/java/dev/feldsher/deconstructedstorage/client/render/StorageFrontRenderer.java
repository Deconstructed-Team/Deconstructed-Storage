package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedcore.common.gui.StorageInventorySlot;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Supplier;

/**
 * What storages show on their front, as 1.21's display item renderer: the items the item display settings pick, one
 * big or up to four small, the lock of a locked storage and the upgrades. Lit as the block the front faces.
 */
public final class StorageFrontRenderer {
	private static final float SMALL_3D_ITEM_SCALE = 0.5F;
	private static final float BIG_2D_ITEM_SCALE = 0.5F;
	private static final float SMALL_2D_ITEM_SCALE = 0.25F;
	private static final float UPGRADE_ITEM_SCALE = 0.125F;
	// GL 1.4's blend factors, which the GL classes here don't name
	private static final int GL_CONSTANT_ALPHA = 0x8003;
	private static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
	private static final Supplier<ItemStack> EMPTY_UPGRADE_STACK = () -> new ItemStack(ModItems.UPGRADE_BASE);

	private StorageFrontRenderer() {}

	/** From the block's center, turned so that its front faces north, as 1.21's north based rotation turns it. */
	public static void rotateToFront(EnumFacing facing) {
		switch (facing) {
			case DOWN:
				GlStateManager.rotate(-90, 1, 0, 0);
				break;
			case UP:
				GlStateManager.rotate(90, 1, 0, 0);
				break;
			case SOUTH:
				GlStateManager.rotate(180, 0, 1, 0);
				break;
			case WEST:
				GlStateManager.rotate(90, 0, 1, 0);
				break;
			case EAST:
				GlStateManager.rotate(-90, 0, 1, 0);
				break;
			default:
				break;
		}
	}

	/**
	 * From the block's center, turned so that its front faces north with its top up, for a front whose top looks
	 * another way than a barrel's would, as a limited barrel's facing up or down.
	 */
	public static void rotateToFront(EnumFacing front, EnumFacing up) {
		// left of one looking at it: the front crossed with its top
		float leftX = front.getYOffset() * up.getZOffset() - front.getZOffset() * up.getYOffset();
		float leftY = front.getZOffset() * up.getXOffset() - front.getXOffset() * up.getZOffset();
		float leftZ = front.getXOffset() * up.getYOffset() - front.getYOffset() * up.getXOffset();
		FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
		// columns: where x, y and z of the north facing front go
		matrix.put(new float[] {leftX, leftY, leftZ, 0, up.getXOffset(), up.getYOffset(), up.getZOffset(), 0,
				-front.getXOffset(), -front.getYOffset(), -front.getZOffset(), 0, 0, 0, 0, 1});
		matrix.flip();
		GlStateManager.multMatrix(matrix);
	}

	/** Light as of the block in front, which the front shows to: the storage's own is that of a solid block, dark. */
	public static void setFrontLight(World world, BlockPos pos, EnumFacing facing) {
		int light = world.getCombinedLight(pos.offset(facing), 0);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536F);
	}

	/**
	 * The display items, with the front's lower left corner at the origin, facing -z; yCenter is where on the front an
	 * item alone is centered, half way up but for a chest's lower front.
	 */
	public static void renderDisplayItems(RenderInfo renderInfo, double yCenter, int maxItems) {
		List<RenderInfo.DisplayItem> displayItems = renderInfo.getItemDisplayRenderInfo().getDisplayItems();
		int count = Math.min(displayItems.size(), maxItems);
		for (int index = 0; index < count; index++) {
			RenderInfo.DisplayItem displayItem = displayItems.get(index);
			renderItem(displayItem.getItem(), displayItem.getRotation(), index, count, (float) yCenter);
		}
	}

	public static void renderItem(ItemStack stack, int rotation, int index, int count, float yCenter) {
		if (stack.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getMinecraft();
		IBakedModel model = minecraft.getRenderItem().getItemModelWithOverrides(stack, null, null);
		float[] offset = getFrontOffset(index, count, yCenter);
		GlStateManager.pushMatrix();
		GlStateManager.translate(offset[0], offset[1], 0);
		// as an item frame shows its item, facing out of the front
		GlStateManager.rotate(180, 0, 1, 0);
		GlStateManager.rotate(rotation, 0, 0, 1);
		float scale = count == 1 ? (model.isGui3d() ? 1.0F : BIG_2D_ITEM_SCALE) : (model.isGui3d() ? SMALL_3D_ITEM_SCALE : SMALL_2D_ITEM_SCALE);
		GlStateManager.scale(scale, scale, scale);
		GlStateManager.pushAttrib();
		RenderHelper.enableStandardItemLighting();
		minecraft.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popAttrib();
		GlStateManager.popMatrix();
	}

	/** Where on the front the item goes, as 1.21 lays out one to four items. */
	public static float[] getFrontOffset(int index, int count, float yCenter) {
		float half = yCenter / 2;
		switch (count) {
			case 1:
				return new float[] {0.5F, yCenter};
			case 2:
				return new float[] {0.5F, index == 0 ? yCenter + half : half};
			case 3:
				return new float[] {index == 0 ? 0.5F : 0.75F - (index - 1) * 0.5F, index == 0 ? yCenter + half : half};
			default:
				return new float[] {index == 0 || index == 2 ? 0.75F : 0.25F, index == 0 || index == 1 ? yCenter + half : half};
		}
	}

	/** An X where a slot that can't be used would show its item, as compression leaves some. */
	public static void renderInaccessibleSlots(List<Integer> inaccessibleSlots, int count, float yCenter) {
		if (inaccessibleSlots.isEmpty()) {
			return;
		}
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sprite = minecraft.getTextureMapBlocks().getAtlasSprite(StorageInventorySlot.INACCESSIBLE_SLOT_BACKGROUND);
		float halfSize = (count == 1 ? BIG_2D_ITEM_SCALE : SMALL_2D_ITEM_SCALE) / 2;
		for (int index = 0; index < count; index++) {
			if (inaccessibleSlots.contains(index)) {
				float[] offset = getFrontOffset(index, count, yCenter);
				renderQuad(sprite, offset[0] - halfSize, offset[1] - halfSize, offset[0] + halfSize, offset[1] + halfSize, -0.001F);
			}
		}
	}

	/**
	 * The upgrades, tiny, in a row from the front's lower right corner, each two pixels wide, moved by the offset. Holding
	 * an upgrade or the tool shows the empty upgrade slots too, as see-through upgrade bases; disabled, as the tool set to
	 * toggle them shows hidden ones, they're crossed out.
	 */
	public static void renderUpgradeItems(List<ItemStack> upgradeItems, float offsetX, float offsetY, float offsetZ, boolean renderEmptySlots, boolean renderDisabled) {
		int i = 0;
		for (ItemStack upgradeItem : upgradeItems) {
			if (upgradeItem.isEmpty() && !renderEmptySlots) {
				continue;
			}
			float x = 1F - i * 2 / 16F - 1 / 16F + offsetX;
			float y = 1 / 16F + offsetY;
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, y, offsetZ);
			GlStateManager.pushMatrix();
			GlStateManager.rotate(180, 0, 1, 0);
			GlStateManager.scale(UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE, UPGRADE_ITEM_SCALE);
			if (upgradeItem.isEmpty()) {
				renderTranslucent(EMPTY_UPGRADE_STACK.get(), 0.5F);
			} else {
				renderFixed(upgradeItem);
			}
			GlStateManager.popMatrix();
			if (renderDisabled) {
				Minecraft minecraft = Minecraft.getMinecraft();
				minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
				renderQuad(minecraft.getTextureMapBlocks().getAtlasSprite(StorageInventorySlot.INACCESSIBLE_SLOT_BACKGROUND), -1 / 16F, -1 / 16F, 1 / 16F, 1 / 16F, -0.01F);
			}
			GlStateManager.popMatrix();
			i++;
		}
	}

	private static void renderFixed(ItemStack stack) {
		GlStateManager.pushAttrib();
		RenderHelper.enableStandardItemLighting();
		Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popAttrib();
	}

	/** Item quads carry their own color, so it's the blend that makes them see-through, by a constant alpha. */
	private static void renderTranslucent(ItemStack stack, float alpha) {
		GlStateManager.enableBlend();
		GL14.glBlendColor(1, 1, 1, alpha);
		GlStateManager.blendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
		GlStateManager.depthMask(false);
		renderFixed(stack);
		GlStateManager.depthMask(true);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL14.glBlendColor(1, 1, 1, 1);
		GlStateManager.disableBlend();
	}

	static void renderQuad(TextureAtlasSprite sprite, float x1, float y1, float x2, float y2, float z) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
		buffer.pos(x2, y1, z).tex(sprite.getMinU(), sprite.getMaxV()).normal(0, 0, -1).endVertex();
		buffer.pos(x1, y1, z).tex(sprite.getMaxU(), sprite.getMaxV()).normal(0, 0, -1).endVertex();
		buffer.pos(x1, y2, z).tex(sprite.getMaxU(), sprite.getMinV()).normal(0, 0, -1).endVertex();
		buffer.pos(x2, y2, z).tex(sprite.getMinU(), sprite.getMinV()).normal(0, 0, -1).endVertex();
		tessellator.draw();
	}

	/** The lock, a pixel square on the front, its middle at x 0.5, its bottom at y. */
	public static void renderLock(float y) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		TextureAtlasSprite sprite = minecraft.getTextureMapBlocks().getAtlasSprite(DeconstructedStorage.MOD_ID + ":block/lock");
		float x1 = 0.5F - 0.5F / 16;
		float x2 = 0.5F + 0.5F / 16;
		float y2 = y + 1 / 16F;
		float z = -0.001F;
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
		buffer.pos(x2, y, z).tex(sprite.getMinU(), sprite.getMaxV()).normal(0, 0, -1).endVertex();
		buffer.pos(x1, y, z).tex(sprite.getMaxU(), sprite.getMaxV()).normal(0, 0, -1).endVertex();
		buffer.pos(x1, y2, z).tex(sprite.getMaxU(), sprite.getMinV()).normal(0, 0, -1).endVertex();
		buffer.pos(x2, y2, z).tex(sprite.getMinU(), sprite.getMinV()).normal(0, 0, -1).endVertex();
		tessellator.draw();
	}
}
