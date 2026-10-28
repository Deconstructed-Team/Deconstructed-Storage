package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.ControllerBlockEntity;
import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * While the player holds the storage tool in the link mode, what's linked to a controller is outlined, with a line from
 * the controller to it; the controller the tool remembers is outlined too. Seen through blocks.
 */
public class ControllerRenderer extends TileEntitySpecialRenderer<ControllerBlockEntity> {
	private static final int LINKED_COLOR = 0xBFFF00;
	private static final int REMEMBERED_COLOR = 0x9AC0CD;

	@Override
	public void render(ControllerBlockEntity controller, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		World world = controller.getWorld();
		if (player == null || world == null) {
			return;
		}
		ItemStack tool = getToolInLinkMode(player.getHeldItemMainhand());
		if (tool.isEmpty()) {
			tool = getToolInLinkMode(player.getHeldItemOffhand());
		}
		if (tool.isEmpty()) {
			return;
		}
		BlockPos controllerPos = controller.getPos();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x - controllerPos.getX(), y - controllerPos.getY(), z - controllerPos.getZ());
		Minecraft.getMinecraft().entityRenderer.disableLightmap();
		GlStateManager.disableTexture2D();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.glLineWidth(2);
		if (StorageToolItem.getControllerLink(tool).map(controllerPos::equals).orElse(false)) {
			renderOutline(getShape(world, controllerPos), REMEMBERED_COLOR);
		}
		for (BlockPos linkedPos : controller.getLinkedBlocks()) {
			AxisAlignedBB shape = getShape(world, linkedPos);
			renderLine(controllerPos, shape, LINKED_COLOR);
			renderOutline(shape, LINKED_COLOR);
		}
		GlStateManager.disableBlend();
		GlStateManager.enableDepth();
		GlStateManager.enableLighting();
		GlStateManager.enableTexture2D();
		Minecraft.getMinecraft().entityRenderer.enableLightmap();
		GlStateManager.popMatrix();
	}

	private static ItemStack getToolInLinkMode(ItemStack stack) {
		return stack.getItem() instanceof StorageToolItem && StorageToolItem.getMode(stack) == StorageToolItem.Mode.LINK ? stack : ItemStack.EMPTY;
	}

	/** The block as it's outlined when looked at, in world coordinates. */
	private static AxisAlignedBB getShape(World world, BlockPos pos) {
		return world.getBlockState(pos).getBoundingBox(world, pos).offset(pos);
	}

	private static void renderOutline(AxisAlignedBB shape, int color) {
		RenderGlobal.drawSelectionBoundingBox(shape.grow(0.002), (color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
	}

	private static void renderLine(BlockPos from, AxisAlignedBB to, int color) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
		buffer.pos(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).endVertex();
		buffer.pos(to.getCenter().x, to.getCenter().y, to.getCenter().z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 255).endVertex();
		tessellator.draw();
	}
}
