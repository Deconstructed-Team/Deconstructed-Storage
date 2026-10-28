package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.BarrelRenderData;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.block.VerticalFacing;
import dev.feldsher.deconstructedstorage.common.gui.BlockSide;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlock;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A barrel facing one way, put together from {@link BarrelParts} by its render data: its wood, closed or open, unless
 * it's tinted all over; the tinted core and trim for the colors it has; and the tier's metal over all. As an item, the
 * same from what its stack says, facing up. A flat top is a plain cube, with no rim around the lid. A limited barrel's
 * top is marked out for its slots, and doesn't open.
 */
public class BarrelBakedModel implements IBakedModel {
	private final IBakedModel baseModel;
	private final Block barrel;
	private final ModelRotation rotation;
	private final EnumFacing horizontalDirection;
	private final VerticalFacing verticalFacing;
	private final boolean flatTop;
	private final int limitedSlots;
	private final ItemOverrideList overrides;

	public BarrelBakedModel(IBakedModel baseModel, Block barrel, ModelRotation rotation, EnumFacing horizontalDirection, VerticalFacing verticalFacing,
			boolean flatTop) {
		this.baseModel = baseModel;
		this.barrel = barrel;
		this.rotation = rotation;
		this.horizontalDirection = horizontalDirection;
		this.verticalFacing = verticalFacing;
		this.flatTop = flatTop;
		limitedSlots = barrel instanceof LimitedBarrelBlock ? ((LimitedBarrelBlock) barrel).getNumberOfInventorySlots() : 0;
		overrides = new StackOverrides();
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
		BarrelRenderData data = state instanceof IExtendedBlockState ? ((IExtendedBlockState) state).getValue(BarrelBlock.RENDER_DATA) : null;
		return getQuads(data == null ? BarrelRenderData.DEFAULT : data, side);
	}

	protected List<BakedQuad> getQuads(BarrelRenderData data, @Nullable EnumFacing side) {
		List<BakedQuad> ret = new ArrayList<>();
		WoodType woodType = data.getWoodType();
		Map<BarrelMaterial, MaterialBlock> materials = data.getMaterials();
		if (!(data.hasMainColor() && data.hasAccentColor()) && materials.isEmpty()) {
			WoodType wood = woodType == null ? WoodType.DEFAULT : woodType;
			ret.addAll(limitedSlots > 0 ? BarrelParts.getLimitedWoodQuads(limitedSlots, wood, flatTop, rotation, side)
					: BarrelParts.getWoodQuads(wood, data.isOpen(), flatTop, rotation, side));
		}
		if (data.hasAccentColor()) {
			ret.addAll(limitedSlots > 0 ? BarrelParts.getLimitedTintableAccentQuads(limitedSlots, flatTop, rotation, side)
					: BarrelParts.getTintableAccentQuads(flatTop, rotation, side));
		}
		if (data.hasMainColor()) {
			ret.addAll(limitedSlots > 0 ? BarrelParts.getLimitedTintableMainQuads(limitedSlots, flatTop, rotation, side)
					: BarrelParts.getTintableMainQuads(data.isOpen(), flatTop, rotation, side));
		}
		if (!materials.isEmpty()) {
			addMaterialQuads(ret, data, woodType == null ? WoodType.DEFAULT : woodType, materials, side);
		}
		if (data.showsTier()) {
			ret.addAll(BarrelParts.getTierQuads(barrel, flatTop, rotation, side));
		}
		if (data.isPacked()) {
			ret.addAll(limitedSlots > 0 ? BarrelParts.getLimitedPackedQuads(limitedSlots, flatTop, rotation, side) : BarrelParts.getPackedQuads(flatTop, rotation, side));
		} else if (data.showsLock()) {
			ret.addAll(BarrelParts.getLockQuads(rotation, side));
		}
		return ret;
	}

	/**
	 * Its parts in the blocks' textures: each face takes the blocks' texture of the face it is on the barrel as it would
	 * be facing north. With materials on only the core or only the trim, those alone are baked anew, over the tints.
	 */
	private void addMaterialQuads(List<BakedQuad> ret, BarrelRenderData data, WoodType woodType, Map<BarrelMaterial, MaterialBlock> materials, @Nullable EnumFacing side) {
		EnumFacing spriteSide = getSpriteSide(side);
		Map<String, String> materialTextures = new HashMap<>();
		Set<BarrelMaterial.MaterialModelPart> materialModelParts = EnumSet.noneOf(BarrelMaterial.MaterialModelPart.class);
		materials.forEach((material, block) -> {
			materialModelParts.add(material.getMaterialModelPart());
			String spriteName = MaterialSprites.getSprite(block, spriteSide).getIconName();
			for (BarrelMaterial child : material.getChildren()) {
				materialTextures.put(child.getSerializedName(), spriteName);
			}
		});
		boolean split = materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE) || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM);
		if (!split) {
			ret.addAll(BarrelParts.getMaterialQuads(limitedSlots, woodType, flatTop, BarrelParts.DynamicPart.WHOLE, materialTextures, rotation, side));
			return;
		}
		if (!data.hasMainColor() || materialModelParts.contains(BarrelMaterial.MaterialModelPart.CORE)) {
			ret.addAll(BarrelParts.getMaterialQuads(limitedSlots, woodType, flatTop, BarrelParts.DynamicPart.CORE, materialTextures, rotation, side));
		}
		if (!data.hasAccentColor() || materialModelParts.contains(BarrelMaterial.MaterialModelPart.TRIM)) {
			ret.addAll(BarrelParts.getMaterialQuads(limitedSlots, woodType, flatTop, BarrelParts.DynamicPart.TRIM, materialTextures, rotation, side));
		}
	}

	/** The face of a barrel facing north that this face of it is: what of the blocks' textures it takes. */
	private EnumFacing getSpriteSide(@Nullable EnumFacing side) {
		if (side == null) {
			return EnumFacing.NORTH;
		}
		return BlockSide.fromDirection(side, horizontalDirection, verticalFacing).toDirection(EnumFacing.NORTH, VerticalFacing.NO);
	}

	@Override
	public boolean isAmbientOcclusion() {
		return true;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return BarrelParts.getParticleSprite(Minecraft.getMinecraft().getTextureMapBlocks(), WoodType.DEFAULT);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ItemCameraTransforms getItemCameraTransforms() {
		return baseModel.getItemCameraTransforms();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return overrides;
	}

	/** A stack's barrel, as it would be placed; models kept by what they show, the few a game has. */
	private class StackOverrides extends ItemOverrideList {
		private final Map<BarrelRenderData, IBakedModel> models = new HashMap<>();
		private final Map<BarrelRenderData, IBakedModel> flatTopModels = new HashMap<>();

		StackOverrides() {
			super(Collections.emptyList());
		}

		@Override
		public IBakedModel handleItemState(IBakedModel originalModel, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
			boolean hasMainColor = StorageBlockItem.getMainColor(stack).isPresent();
			boolean hasAccentColor = StorageBlockItem.getAccentColor(stack).isPresent();
			WoodType woodType = WoodStorageBlockItem.getWoodType(stack).orElse(hasMainColor && hasAccentColor ? null : WoodType.DEFAULT);
			BarrelRenderData data = new BarrelRenderData(woodType, hasMainColor, hasAccentColor, false, StorageBlockItem.getContentsUuid(stack).isPresent(), true, false,
					BarrelBlockItem.getMaterials(stack));
			Map<BarrelRenderData, IBakedModel> models = BarrelBlockItem.isFlatTop(stack) ? flatTopModels : this.models;
			return models.computeIfAbsent(data, d -> new BarrelBakedModel(baseModel, barrel, rotation, horizontalDirection, verticalFacing, BarrelBlockItem.isFlatTop(stack)) {
				@Override
				public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
					return getQuads(d, side);
				}

				@Override
				public TextureAtlasSprite getParticleTexture() {
					return BarrelParts.getParticleSprite(Minecraft.getMinecraft().getTextureMapBlocks(), d.getWoodType());
				}
			});
		}
	}
}
