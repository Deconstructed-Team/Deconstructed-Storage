package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import java.util.List;
import java.util.Optional;

/** The textures of a block that a barrel is made of: of each of its faces, as its model has them. */
public final class MaterialSprites {
	private MaterialSprites() {}

	/** The block's texture on that face; its particle texture when its model has no face there. */
	public static TextureAtlasSprite getSprite(MaterialBlock block, EnumFacing face) {
		Optional<IBlockState> state = block.getState();
		if (!state.isPresent()) {
			return Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite();
		}
		IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state.get());
		List<BakedQuad> quads = model.getQuads(state.get(), face, 0);
		if (quads.isEmpty()) {
			for (BakedQuad quad : model.getQuads(state.get(), null, 0)) {
				if (quad.getFace() == face) {
					return quad.getSprite();
				}
			}
			return model.getParticleTexture();
		}
		return quads.get(0).getSprite();
	}

	public static TextureAtlasSprite getParticleSprite(MaterialBlock block) {
		return block.getState().map(state -> Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state).getParticleTexture())
				.orElseGet(() -> Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite());
	}
}
