package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
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
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * What storages show on their front, as 1.21's display item renderer: the items the item display settings pick, one
 * big or up to four small, and the lock of a locked storage. Lit as the block the front faces.
 */
public final class StorageFrontRenderer {
	private static final float SMALL_3D_ITEM_SCALE = 0.5F;
	private static final float BIG_2D_ITEM_SCALE = 0.5F;
	private static final float SMALL_2D_ITEM_SCALE = 0.25F;

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

	private static void renderItem(ItemStack stack, int rotation, int index, int count, float yCenter) {
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
	private static float[] getFrontOffset(int index, int count, float yCenter) {
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
