package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlock;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelShulker;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Draws shulker boxes as 1.21's renderer does, on vanilla's shulker model, which hasn't changed since 1.12: the plain
 * box unless it's tinted all over, the tinted parts in their colors, then the tier's metal. Placed as 1.12's own
 * shulker box renderer places its model, for each facing.
 */
public class ShulkerBoxRenderer extends TileEntitySpecialRenderer<ShulkerBoxBlockEntity> {
	private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
	private final ModelShulker model = new ModelShulker();

	@Override
	public void render(ShulkerBoxBlockEntity box, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		IBlockState state = box.hasWorld() ? box.getWorld().getBlockState(box.getPos()) : null;
		if (state == null || !(state.getBlock() instanceof ShulkerBoxBlock)) {
			return;
		}
		StorageWrapper wrapper = box.getStorageWrapper();
		render(state.getBlock(), state.getValue(ShulkerBoxBlock.FACING), wrapper.hasMainColor() ? wrapper.getMainColor() : -1,
				wrapper.hasAccentColor() ? wrapper.getAccentColor() : -1, box.getProgress(partialTicks), x, y, z, destroyStage);
	}

	/** A shulker box of the block's tier with those colors, -1 for none, at x y z, its lid open by progress, from 0 to 1. */
	public void render(Block shulkerBoxBlock, EnumFacing facing, int mainColor, int accentColor, float progress, double x, double y, double z, int destroyStage) {
		GlStateManager.enableDepth();
		GlStateManager.depthFunc(GL11.GL_LEQUAL);
		GlStateManager.depthMask(true);
		GlStateManager.disableCull();
		if (destroyStage >= 0) {
			bindTexture(DESTROY_STAGES[destroyStage]);
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.pushMatrix();
			GlStateManager.scale(4.0F, 4.0F, 1.0F);
			GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		}

		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.translate(x + 0.5F, y + 1.5F, z + 0.5F);
		GlStateManager.scale(1.0F, -1.0F, -1.0F);
		GlStateManager.translate(0.0F, 1.0F, 0.0F);
		GlStateManager.scale(0.9995F, 0.9995F, 0.9995F);
		GlStateManager.translate(0.0F, -1.0F, 0.0F);
		rotateToFacing(facing);

		if (destroyStage >= 0) {
			renderModel(progress);
		} else {
			if (mainColor == -1 || accentColor == -1) {
				bindTexture(getTexture("no_tint"));
				renderModel(progress);
			}
			if (mainColor != -1) {
				renderTinted(progress, "tintable_main", mainColor);
			}
			if (accentColor != -1) {
				renderTinted(progress, "tintable_accent", accentColor);
			}
			bindTexture(getTexture(getTierName(shulkerBoxBlock) + "_tier"));
			renderModel(progress);
		}

		GlStateManager.enableCull();
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
		GlStateManager.color(1, 1, 1, 1);

		if (destroyStage >= 0) {
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		}
	}

	/** As TileEntityShulkerBoxRenderer turns its model: it's modelled facing up, upside down. */
	private static void rotateToFacing(EnumFacing facing) {
		switch (facing) {
			case DOWN:
				GlStateManager.translate(0.0F, 2.0F, 0.0F);
				GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
				break;
			case NORTH:
				GlStateManager.translate(0.0F, 1.0F, 1.0F);
				GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
				break;
			case SOUTH:
				GlStateManager.translate(0.0F, 1.0F, -1.0F);
				GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				break;
			case WEST:
				GlStateManager.translate(-1.0F, 1.0F, 0.0F);
				GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
				break;
			case EAST:
				GlStateManager.translate(1.0F, 1.0F, 0.0F);
				GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
				GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
				break;
			default:
				break;
		}
	}

	private void renderModel(float progress) {
		model.base.render(1 / 16F);
		GlStateManager.pushMatrix();
		GlStateManager.translate(0.0F, -progress * 0.5F, 0.0F);
		GlStateManager.rotate(270.0F * progress, 0.0F, 1.0F, 0.0F);
		model.lid.render(1 / 16F);
		GlStateManager.popMatrix();
	}

	private void renderTinted(float progress, String texture, int color) {
		bindTexture(getTexture(texture));
		GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
		renderModel(progress);
		GlStateManager.color(1, 1, 1, 1);
	}

	private static ResourceLocation getTexture(String name) {
		return TEXTURES.computeIfAbsent(name, n -> new ResourceLocation(DeconstructedStorage.MOD_ID, "textures/entity/shulker_box/" + n + ".png"));
	}

	/** "base" for the basic tier, whose boxes are named only "shulker_box"; else what the name starts with. */
	private static String getTierName(Block shulkerBoxBlock) {
		String path = Objects.requireNonNull(shulkerBoxBlock.getRegistryName()).getPath();
		return path.endsWith("_shulker_box") ? path.substring(0, path.length() - "_shulker_box".length()) : "base";
	}
}
