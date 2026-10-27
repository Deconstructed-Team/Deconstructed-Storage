package dev.feldsher.deconstructedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.WoodType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The parts a barrel's model is put together from, as 1.21's barrel model has them: the wood, closed or open, or the
 * tinted core and trim in its place, and the tier's metal over them. Each part is its model file retextured for a
 * wood or a tier, baked turned for each facing, its quads kept by the face they're culled with.
 */
public final class BarrelParts {
	private static final String BLOCK = DeconstructedStorage.MOD_ID + ":block/";
	private static final ResourceLocation BASE = DeconstructedStorage.getRL("block/barrel_part/base");
	private static final ResourceLocation TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/tintable_main");
	private static final ResourceLocation TINTABLE_ACCENT = DeconstructedStorage.getRL("block/barrel_part/tintable_accent");
	// a flat top is a plain cube: no rim around the lid
	private static final ResourceLocation FLAT_BASE = new ResourceLocation("block/cube_bottom_top");
	private static final ResourceLocation FLAT_TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/flat_tintable_main");
	private static final ResourceLocation FLAT_TINTABLE_ACCENT = DeconstructedStorage.getRL("block/barrel_part/flat_tintable_accent");
	/** The tints of the tinted parts, as 1.21's model files have them: the main color and the accent. */
	public static final int MAIN_TINT = 1000;
	public static final int ACCENT_TINT = 1001;

	private static final Map<String, Map<EnumFacing, Map<EnumFacing, List<BakedQuad>>>> BAKED = new HashMap<>();

	private BarrelParts() {}

	/** Facings as the blockstate files turn them: up as modelled, the rest tipped over onto their side. */
	public static ModelRotation getRotation(EnumFacing facing) {
		switch (facing) {
			case DOWN:
				return ModelRotation.X180_Y0;
			case NORTH:
				return ModelRotation.X90_Y0;
			case SOUTH:
				return ModelRotation.X90_Y180;
			case WEST:
				return ModelRotation.X90_Y270;
			case EAST:
				return ModelRotation.X90_Y90;
			default:
				return ModelRotation.X0_Y0;
		}
	}

	/** The baked parts go with the models they were baked from. */
	public static void clear() {
		BAKED.clear();
	}

	/**
	 * Every texture the parts use: the part models are loaded only as they're baked, after the atlas is made, so it would
	 * have none of theirs, nor those they're retextured with.
	 */
	public static void registerSprites(TextureMap map) {
		for (WoodType woodType : WoodType.values()) {
			for (String part : new String[] {"top", "top_open", "side", "bottom"}) {
				map.registerSprite(new ResourceLocation(BLOCK + woodType.getName() + "_barrel_" + part));
			}
		}
		for (String tier : new String[] {"wood", "copper", "iron", "gold", "diamond", "netherite"}) {
			for (String part : new String[] {"top", "side", "bottom"}) {
				map.registerSprite(new ResourceLocation(BLOCK + tier + "_barrel_" + part));
			}
		}
		for (String part : new String[] {"top", "side", "bottom"}) {
			map.registerSprite(new ResourceLocation(BLOCK + "barrel_" + part + "_tintable_main"));
			map.registerSprite(new ResourceLocation(BLOCK + "barrel_" + part + "_tintable_accent"));
		}
		map.registerSprite(new ResourceLocation(BLOCK + "barrel_top_tintable_main_open"));
		map.registerSprite(new ResourceLocation(BLOCK + "barrel_handle"));
		for (String part : new String[] {"top", "side", "bottom"}) {
			map.registerSprite(new ResourceLocation(BLOCK + "barrel_" + part + "_packed"));
		}
		map.registerSprite(new ResourceLocation(BLOCK + "barrel_metal_bands"));
	}

	public static List<BakedQuad> getWoodQuads(WoodType woodType, boolean open, boolean flatTop, EnumFacing facing, @Nullable EnumFacing side) {
		String prefix = BLOCK + woodType.getName() + "_barrel_";
		String top = prefix + (open ? "top_open" : "top");
		return getQuads(flatPrefix(flatTop) + "wood_" + woodType.getName() + (open ? "_open" : ""), flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.<String, String>builder()
				.put("top", top).put("top_trim", top).put("top_inner_trim", top)
				.put("side", prefix + "side").put("side_trim", prefix + "side")
				.put("bottom", prefix + "bottom").put("bottom_trim", prefix + "bottom")
				.put("particle", prefix + "top").build(), facing, side);
	}

	public static List<BakedQuad> getTierQuads(Block barrel, boolean flatTop, EnumFacing facing, @Nullable EnumFacing side) {
		String tier = getTierName(barrel);
		String prefix = BLOCK + tier + "_barrel_";
		return getQuads(flatPrefix(flatTop) + "tier_" + tier, flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.of("top", prefix + "top", "top_trim", prefix + "top",
				"side", prefix + "side", "bottom", prefix + "bottom"), facing, side);
	}

	/** Tape around it, as a packed barrel has. */
	public static List<BakedQuad> getPackedQuads(boolean flatTop, EnumFacing facing, @Nullable EnumFacing side) {
		return getQuads(flatPrefix(flatTop) + "packed", flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.of("top", BLOCK + "barrel_top_packed",
				"top_trim", BLOCK + "barrel_top_packed", "side", BLOCK + "barrel_side_packed", "bottom", BLOCK + "barrel_bottom_packed"), facing, side);
	}

	public static List<BakedQuad> getTintableMainQuads(boolean open, boolean flatTop, EnumFacing facing, @Nullable EnumFacing side) {
		ResourceLocation model = flatTop ? FLAT_TINTABLE_MAIN : TINTABLE_MAIN;
		return open ? getQuads(flatPrefix(flatTop) + "tintable_main_open", model, () -> ImmutableMap.of("top", BLOCK + "barrel_top_tintable_main_open",
				"particle", BLOCK + "barrel_top_tintable_main_open"), facing, side)
				: getQuads(flatPrefix(flatTop) + "tintable_main", model, ImmutableMap::of, facing, side);
	}

	public static List<BakedQuad> getTintableAccentQuads(boolean flatTop, EnumFacing facing, @Nullable EnumFacing side) {
		return getQuads(flatPrefix(flatTop) + "tintable_accent", flatTop ? FLAT_TINTABLE_ACCENT : TINTABLE_ACCENT, ImmutableMap::of, facing, side);
	}

	private static String flatPrefix(boolean flatTop) {
		return flatTop ? "flat_" : "";
	}

	/** The wood's sprite that particles take, as 1.21's barrels do theirs: the top. */
	public static TextureAtlasSprite getParticleSprite(TextureMap map, @Nullable WoodType woodType) {
		return map.getAtlasSprite(woodType == null ? BLOCK + "barrel_top_tintable_main" : BLOCK + woodType.getName() + "_barrel_top");
	}

	/** "wood" for the basic tier, whose barrels are named only "barrel"; else what the name starts with. */
	private static String getTierName(Block barrel) {
		String path = Objects.requireNonNull(barrel.getRegistryName()).getPath();
		return barrel instanceof BarrelBlock && path.endsWith("_barrel") ? path.substring(0, path.length() - "_barrel".length()) : "wood";
	}

	private static List<BakedQuad> getQuads(String key, ResourceLocation model, java.util.function.Supplier<ImmutableMap<String, String>> textures,
			EnumFacing facing, @Nullable EnumFacing side) {
		Map<EnumFacing, List<BakedQuad>> bySide = BAKED.computeIfAbsent(key, k -> new EnumMap<>(EnumFacing.class))
				.computeIfAbsent(facing, f -> bake(model, textures.get(), f));
		List<BakedQuad> quads = side == null ? bySide.get(null) : bySide.get(side);
		return quads == null ? Collections.emptyList() : quads;
	}

	/** By the face they're culled with; the unculled ones under null, which an EnumMap can't hold, so a HashMap. */
	private static Map<EnumFacing, List<BakedQuad>> bake(ResourceLocation modelLocation, ImmutableMap<String, String> textures, EnumFacing facing) {
		IModel model = ModelLoaderRegistry.getModelOrMissing(modelLocation).retexture(textures);
		IBakedModel baked = model.bake(getRotation(facing), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		Map<EnumFacing, List<BakedQuad>> bySide = new HashMap<>();
		bySide.put(null, new ArrayList<>(baked.getQuads(null, null, 0)));
		for (EnumFacing side : EnumFacing.values()) {
			bySide.put(side, new ArrayList<>(baked.getQuads(null, side, 0)));
		}
		return bySide;
	}
}
