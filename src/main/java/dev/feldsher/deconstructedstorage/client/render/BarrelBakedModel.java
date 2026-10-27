package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelRenderData;
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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A barrel facing one way, put together from {@link BarrelParts} by its render data: its wood, closed or open, unless
 * it's tinted all over; the tinted core and trim for the colors it has; and the tier's metal over all. As an item, the
 * same from what its stack says, facing up. A flat top is a plain cube, with no rim around the lid.
 */
public class BarrelBakedModel implements IBakedModel {
	private final IBakedModel baseModel;
	private final Block barrel;
	private final EnumFacing facing;
	private final boolean flatTop;
	private final ItemOverrideList overrides;

	public BarrelBakedModel(IBakedModel baseModel, Block barrel, EnumFacing facing, boolean flatTop) {
		this.baseModel = baseModel;
		this.barrel = barrel;
		this.facing = facing;
		this.flatTop = flatTop;
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
		if (!(data.hasMainColor() && data.hasAccentColor())) {
			ret.addAll(BarrelParts.getWoodQuads(woodType == null ? WoodType.DEFAULT : woodType, data.isOpen(), flatTop, facing, side));
		}
		if (data.hasAccentColor()) {
			ret.addAll(BarrelParts.getTintableAccentQuads(flatTop, facing, side));
		}
		if (data.hasMainColor()) {
			ret.addAll(BarrelParts.getTintableMainQuads(data.isOpen(), flatTop, facing, side));
		}
		if (data.showsTier()) {
			ret.addAll(BarrelParts.getTierQuads(barrel, flatTop, facing, side));
		}
		if (data.isPacked()) {
			ret.addAll(BarrelParts.getPackedQuads(flatTop, facing, side));
		} else if (data.showsLock()) {
			ret.addAll(BarrelParts.getLockQuads(facing, side));
		}
		return ret;
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
			BarrelRenderData data = new BarrelRenderData(woodType, hasMainColor, hasAccentColor, false, StorageBlockItem.getContentsUuid(stack).isPresent(), true, false);
			Map<BarrelRenderData, IBakedModel> models = BarrelBlockItem.isFlatTop(stack) ? flatTopModels : this.models;
			return models.computeIfAbsent(data, d -> new BarrelBakedModel(baseModel, barrel, facing, BarrelBlockItem.isFlatTop(stack)) {
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
