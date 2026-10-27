package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestType;
import dev.feldsher.deconstructedcore.renderdata.DisplaySide;
import dev.feldsher.deconstructedcore.renderdata.RenderInfo;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import dev.feldsher.deconstructedstorage.block.WoodType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Draws chests as 1.21's chest renderer does: the wood, or the tinted core and trim in its place, then the tier's metal
 * over them, and the lock. Their textures are laid out for 1.21's chest model, which 1.15 turned the right way up:
 * so is the model here, made of 1.12's model boxes, which unwrap a texture as 1.21's cubes do, and drawn without the
 * flip 1.12's own chest needs.
 */
public class ChestRenderer extends TileEntitySpecialRenderer<ChestBlockEntity> {
	private static final Map<ChestType, Parts> PARTS = new EnumMap<>(ChestType.class);
	private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();

	static {
		PARTS.put(ChestType.SINGLE, new Parts(1, 14, 7, 15, 2));
		PARTS.put(ChestType.RIGHT, new Parts(1, 15, 15, 14, 1));
		PARTS.put(ChestType.LEFT, new Parts(0, 15, 0, 14, 1));
	}

	/** A chest's bottom, lid and lock, as 1.21's chest layers have them for a single chest and each half of a double one. */
	private static class Parts extends ModelBase {
		private final ModelRenderer bottom;
		private final ModelRenderer lid;
		private final ModelRenderer lock;

		Parts(int boxX, int width, int lockX, int lockZ, int lockWidth) {
			textureWidth = 64;
			textureHeight = 64;
			bottom = new ModelRenderer(this, 0, 19);
			bottom.addBox(boxX, 0, 1, width, 10, 14);
			lid = new ModelRenderer(this, 0, 0);
			lid.addBox(boxX, 0, 0, width, 5, 14);
			lid.setRotationPoint(0, 9, 1);
			lock = new ModelRenderer(this, 0, 0);
			if (lockWidth == 2) {
				lock.addBox(lockX, -1, lockZ, 2, 4, 1);
				lock.setRotationPoint(0, 8, 0);
			} else {
				lock.addBox(lockX, -2, lockZ, 1, 4, 1);
				lock.setRotationPoint(0, 9, 1);
			}
		}

		void setLidAngle(float lidAngle) {
			lid.rotateAngleX = -(lidAngle * ((float) Math.PI / 2F));
			lock.rotateAngleX = lid.rotateAngleX;
		}
	}

	@Override
	public void render(ChestBlockEntity chest, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		IBlockState state = chest.hasWorld() ? chest.getWorld().getBlockState(chest.getPos()) : null;
		if (state == null || !(state.getBlock() instanceof ChestBlock)) {
			return;
		}
		StorageWrapper wrapper = chest.getStorageWrapper();
		float lidAngle = chest.getOpenness(partialTicks);
		lidAngle = 1.0F - lidAngle;
		lidAngle = 1.0F - lidAngle * lidAngle * lidAngle;
		StorageBlockEntity main = chest.getMainBlockEntity();
		render(state.getBlock(), state.getValue(ChestBlock.TYPE), state.getValue(ChestBlock.FACING), chest.getWoodType().orElse(null),
				wrapper.hasMainColor() ? wrapper.getMainColor() : -1, wrapper.hasAccentColor() ? wrapper.getAccentColor() : -1, chest.isPacked(), main.shouldShowTier(),
				lidAngle, x, y, z, destroyStage);
		if (destroyStage < 0 && !chest.isPacked() && main == chest) {
			renderFront(chest, state, lidAngle, x, y, z);
		}
	}

	/**
	 * The main half's lock and display item, on its front, or a double chest's middle, as 1.21's chest renderer puts
	 * them; the lock goes up with the lid.
	 */
	private void renderFront(ChestBlockEntity chest, IBlockState state, float lidAngle, double x, double y, double z) {
		ChestType chestType = state.getValue(ChestBlock.TYPE);
		EnumFacing facing = state.getValue(ChestBlock.FACING);
		RenderInfo renderInfo = chest.getStorageWrapper().getRenderInfo();
		Optional<RenderInfo.DisplayItem> displayItem = renderInfo.getItemDisplayRenderInfo().getDisplayItem();
		boolean showsLock = chest.isLocked() && chest.shouldShowLock();
		if (!displayItem.isPresent() && !showsLock) {
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
		GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
		GlStateManager.rotate(180, 0, 1, 0);
		StorageFrontRenderer.setFrontLight(chest.getWorld(), chest.getPos(), facing);
		if (showsLock) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(-0.5, -0.5, -(0.5 - 1 / 16D));
			if (lidAngle > 0) {
				GlStateManager.translate(0, 9 / 16D, 14 / 16D);
				GlStateManager.rotate(lidAngle * 90, 1, 0, 0);
				GlStateManager.translate(0, -9 / 16D, -14 / 16D);
			}
			GlStateManager.translate(chestType == ChestType.LEFT ? 0.5 : chestType == ChestType.RIGHT ? -0.5 : 0, 0, 0);
			StorageFrontRenderer.renderLock(13 / 16F);
			GlStateManager.popMatrix();
		}
		displayItem.ifPresent(item -> {
			DisplaySide displaySide = item.getDisplaySide();
			if (displaySide == DisplaySide.LEFT) {
				GlStateManager.rotate(-90, 0, 1, 0);
				if (chestType == ChestType.LEFT) {
					GlStateManager.translate(0, 0, -1);
				}
			} else if (displaySide == DisplaySide.RIGHT) {
				GlStateManager.rotate(90, 0, 1, 0);
				if (chestType == ChestType.RIGHT) {
					GlStateManager.translate(0, 0, -1);
				}
			} else if (chestType != ChestType.SINGLE) {
				GlStateManager.translate(chestType == ChestType.RIGHT ? -0.5 : 0.5, 0, 0);
			}
			GlStateManager.translate(-0.5, -0.5, -(0.5 - 1 / 16D));
			StorageFrontRenderer.renderDisplayItems(renderInfo, 0.5 * (14.01 / 16), 1);
		});
		GlStateManager.popMatrix();
	}

	/** A chest of the block's tier with that look, at x y z, its lid open by lidAngle, from 0 to 1. */
	public void render(Block chestBlock, ChestType chestType, EnumFacing facing, @Nullable WoodType woodType, int mainColor, int accentColor, boolean packed,
			boolean showsTier, float lidAngle, double x, double y, double z, int destroyStage) {
		Parts parts = PARTS.get(chestType);
		parts.setLidAngle(lidAngle);
		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.translate(x, y, z);
		GlStateManager.translate(0.5F, 0.5F, 0.5F);
		GlStateManager.rotate(-facing.getHorizontalAngle(), 0, 1, 0);
		GlStateManager.translate(-0.5F, -0.5F, -0.5F);

		if (destroyStage >= 0) {
			bindTexture(DESTROY_STAGES[destroyStage]);
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.pushMatrix();
			GlStateManager.scale(4.0F, 4.0F, 1.0F);
			GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
			renderBottomAndLid(parts);
			GlStateManager.matrixMode(GL11.GL_TEXTURE);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		} else {
			if (woodType != null || mainColor == -1 || accentColor == -1) {
				bindTexture(getTexture(chestType, (woodType == null ? WoodType.DEFAULT : woodType).getName()));
				renderBottomAndLid(parts);
			}
			if (mainColor != -1) {
				renderTinted(parts, chestType, "tintable_main", mainColor);
			}
			if (accentColor != -1) {
				renderTinted(parts, chestType, "tintable_accent", accentColor);
			}
			bindTexture(getTexture(chestType, getTierName(chestBlock) + "_tier"));
			if (showsTier) {
				renderBottomAndLid(parts);
			}
			parts.lock.render(1 / 16F);
			if (packed) {
				// the tape a hair bigger than the chest, around it
				GlStateManager.pushMatrix();
				GlStateManager.translate(-0.005F, -0.005F, -0.005F);
				GlStateManager.scale(1.01F, 1.01F, 1.01F);
				bindTexture(getTexture(chestType, "packed"));
				renderBottomAndLid(parts);
				GlStateManager.popMatrix();
			}
		}

		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
		GlStateManager.color(1, 1, 1, 1);
	}

	private void renderTinted(Parts parts, ChestType chestType, String texture, int color) {
		bindTexture(getTexture(chestType, texture));
		GlStateManager.color((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F, 1);
		renderBottomAndLid(parts);
		GlStateManager.color(1, 1, 1, 1);
	}

	/** The lid a hair bigger while it's open, as 1.21 draws it, so that it doesn't flicker into the bottom. */
	private static void renderBottomAndLid(Parts parts) {
		if (parts.lid.rotateAngleX != 0) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(-0.0005F, -0.001F, -0.0005F);
			GlStateManager.scale(1.001F, 1.001F, 1.001F);
			parts.lid.render(1 / 16F);
			GlStateManager.popMatrix();
		} else {
			parts.lid.render(1 / 16F);
		}
		parts.bottom.render(1 / 16F);
	}

	private static ResourceLocation getTexture(ChestType chestType, String name) {
		String prefix = chestType == ChestType.LEFT ? "left_" : chestType == ChestType.RIGHT ? "right_" : "";
		return TEXTURES.computeIfAbsent(prefix + name, n -> new ResourceLocation(DeconstructedStorage.MOD_ID, "textures/entity/chest/" + n + ".png"));
	}

	/** "wood" for the basic tier, whose chests are named only "chest"; else what the name starts with. */
	private static String getTierName(Block chestBlock) {
		String path = Objects.requireNonNull(chestBlock.getRegistryName()).getPath();
		return path.endsWith("_chest") ? path.substring(0, path.length() - "_chest".length()) : "wood";
	}
}
