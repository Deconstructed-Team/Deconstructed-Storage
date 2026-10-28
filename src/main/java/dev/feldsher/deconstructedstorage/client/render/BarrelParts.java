package dev.feldsher.deconstructedstorage.client.render;

import com.google.common.collect.ImmutableMap;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.VerticalFacing;
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
 * wood or a tier, baked turned for each facing, its quads kept by the face they're culled with. A limited barrel's
 * parts have the trims between its slots along, which the parts' own models are put together with.
 */
public final class BarrelParts {
	private static final String BLOCK = DeconstructedStorage.MOD_ID + ":block/";
	private static final ResourceLocation BASE = DeconstructedStorage.getRL("block/barrel_part/base");
	private static final ResourceLocation TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/tintable_main");
	private static final ResourceLocation LOCKED = DeconstructedStorage.getRL("block/barrel_part/locked");
	private static final ResourceLocation TINTABLE_ACCENT = DeconstructedStorage.getRL("block/barrel_part/tintable_accent");
	// a flat top is a plain cube: no rim around the lid
	private static final ResourceLocation FLAT_BASE = new ResourceLocation("block/cube_bottom_top");
	private static final ResourceLocation FLAT_TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/flat_tintable_main");
	private static final ResourceLocation FLAT_TINTABLE_ACCENT = DeconstructedStorage.getRL("block/barrel_part/flat_tintable_accent");
	private static final ResourceLocation LIMITED_TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/limited_tintable_main");
	private static final ResourceLocation FLAT_LIMITED_TINTABLE_MAIN = DeconstructedStorage.getRL("block/barrel_part/flat_limited_tintable_main");
	// the trims between a limited barrel's slots: across the middle, then down the lower half, then down the upper
	private static final String[] INNER_TRIMS = {"middle_inner_trim", "down_inner_trim", "up_inner_trim"};
	/** The tints of the tinted parts, as 1.21's model files have them: the main color and the accent. */
	public static final int MAIN_TINT = 1000;
	public static final int ACCENT_TINT = 1001;
	public static final String FILL_INDICATORS = BLOCK + "fill_indicators";

	private static final Map<String, Map<ModelRotation, Map<EnumFacing, List<BakedQuad>>>> BAKED = new HashMap<>();

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

	/**
	 * A limited barrel's turn: facing a side as a barrel does; facing up or down, turned about its front by its
	 * horizontal facing, which is where the lower slots are.
	 */
	public static ModelRotation getLimitedRotation(EnumFacing horizontalFacing, VerticalFacing verticalFacing) {
		int x = verticalFacing == VerticalFacing.NO ? 90 : verticalFacing == VerticalFacing.UP ? 0 : 180;
		return ModelRotation.getModelRotation(x, (horizontalFacing.getHorizontalIndex() + 2) % 4 * 90);
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
		map.registerSprite(new ResourceLocation(BLOCK + "lock"));
		for (int slots = 1; slots <= 4; slots++) {
			for (WoodType woodType : WoodType.values()) {
				map.registerSprite(new ResourceLocation(BLOCK + "limited_" + woodType.getName() + "_barrel_" + slots + "_top"));
			}
			map.registerSprite(new ResourceLocation(BLOCK + "limited_barrel_" + slots + "_top_tintable_main"));
			map.registerSprite(new ResourceLocation(BLOCK + "limited_barrel_" + slots + "_top_tintable_accent"));
		}
		map.registerSprite(new ResourceLocation(FILL_INDICATORS));
		map.registerSprite(new ResourceLocation(BLOCK + "barrel_metal_bands"));
	}

	public static List<BakedQuad> getWoodQuads(WoodType woodType, boolean open, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		String prefix = BLOCK + woodType.getName() + "_barrel_";
		String top = prefix + (open ? "top_open" : "top");
		return getQuads(flatPrefix(flatTop) + "wood_" + woodType.getName() + (open ? "_open" : ""), flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.<String, String>builder()
				.put("top", top).put("top_trim", top).put("top_inner_trim", top)
				.put("side", prefix + "side").put("side_trim", prefix + "side")
				.put("bottom", prefix + "bottom").put("bottom_trim", prefix + "bottom")
				.put("particle", prefix + "top").build(), rotation, side);
	}

	public static List<BakedQuad> getTierQuads(Block barrel, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		String tier = getTierName(barrel);
		String prefix = BLOCK + tier + "_barrel_";
		return getQuads(flatPrefix(flatTop) + "tier_" + tier, flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.of("top", prefix + "top", "top_trim", prefix + "top",
				"side", prefix + "side", "bottom", prefix + "bottom"), rotation, side);
	}

	/** The lock on its top edge, as a locked barrel shows. */
	public static List<BakedQuad> getLockQuads(ModelRotation rotation, @Nullable EnumFacing side) {
		return getQuads("locked", LOCKED, ImmutableMap::of, rotation, side);
	}

	/** Tape around it, as a packed barrel has. */
	public static List<BakedQuad> getPackedQuads(boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		return getQuads(flatPrefix(flatTop) + "packed", flatTop ? FLAT_BASE : BASE, () -> ImmutableMap.of("top", BLOCK + "barrel_top_packed",
				"top_trim", BLOCK + "barrel_top_packed", "side", BLOCK + "barrel_side_packed", "bottom", BLOCK + "barrel_bottom_packed"), rotation, side);
	}

	public static List<BakedQuad> getTintableMainQuads(boolean open, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		ResourceLocation model = flatTop ? FLAT_TINTABLE_MAIN : TINTABLE_MAIN;
		return open ? getQuads(flatPrefix(flatTop) + "tintable_main_open", model, () -> ImmutableMap.of("top", BLOCK + "barrel_top_tintable_main_open",
				"particle", BLOCK + "barrel_top_tintable_main_open"), rotation, side)
				: getQuads(flatPrefix(flatTop) + "tintable_main", model, ImmutableMap::of, rotation, side);
	}

	public static List<BakedQuad> getTintableAccentQuads(boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		return getQuads(flatPrefix(flatTop) + "tintable_accent", flatTop ? FLAT_TINTABLE_ACCENT : TINTABLE_ACCENT, ImmutableMap::of, rotation, side);
	}

	/** A limited barrel's wood: its top marked out for its slots, the trims between them standing out. */
	public static List<BakedQuad> getLimitedWoodQuads(int slots, WoodType woodType, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		String top = BLOCK + "limited_" + woodType.getName() + "_barrel_" + slots + "_top";
		String prefix = BLOCK + woodType.getName() + "_barrel_";
		return getQuads(flatPrefix(flatTop) + "limited_" + slots + "_wood_" + woodType.getName(), getLimitedModels(slots, flatTop, flatTop ? FLAT_BASE : BASE, ""),
				() -> ImmutableMap.<String, String>builder().put("top", top).put("top_trim", top).put("top_inner_trim", top)
						.put("side", prefix + "side").put("side_trim", prefix + "side").put("bottom", prefix + "bottom").put("bottom_trim", prefix + "bottom")
						.put("particle", top).build(), rotation, side);
	}

	public static List<BakedQuad> getLimitedTintableMainQuads(int slots, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		String top = BLOCK + "limited_barrel_" + slots + "_top_tintable_main";
		return getQuads(flatPrefix(flatTop) + "limited_" + slots + "_tintable_main", Collections.singletonList(flatTop ? FLAT_LIMITED_TINTABLE_MAIN : LIMITED_TINTABLE_MAIN),
				() -> ImmutableMap.of("top", top, "particle", top), rotation, side);
	}

	public static List<BakedQuad> getLimitedTintableAccentQuads(int slots, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		String top = BLOCK + "limited_barrel_" + slots + "_top_tintable_accent";
		return getQuads(flatPrefix(flatTop) + "limited_" + slots + "_tintable_accent", getLimitedModels(slots, flatTop, flatTop ? FLAT_TINTABLE_ACCENT : TINTABLE_ACCENT, "_tintable_accent"),
				() -> ImmutableMap.of("top", top, "top_trim", top, "top_inner_trim", top, "particle", top), rotation, side);
	}

	public static List<BakedQuad> getLimitedPackedQuads(int slots, boolean flatTop, ModelRotation rotation, @Nullable EnumFacing side) {
		return getQuads(flatPrefix(flatTop) + "limited_" + slots + "_packed", getLimitedModels(slots, flatTop, flatTop ? FLAT_BASE : BASE, ""),
				() -> ImmutableMap.<String, String>builder().put("top", BLOCK + "barrel_top_packed").put("top_trim", BLOCK + "barrel_top_packed")
						.put("top_inner_trim", BLOCK + "barrel_top_packed").put("side", BLOCK + "barrel_side_packed").put("bottom", BLOCK + "barrel_bottom_packed").build(),
				rotation, side);
	}

	/** The part, with a trim between each two slots; a flat top has none, its texture marks the slots out. */
	private static List<ResourceLocation> getLimitedModels(int slots, boolean flatTop, ResourceLocation base, String trimSuffix) {
		List<ResourceLocation> models = new ArrayList<>();
		models.add(base);
		if (!flatTop) {
			for (int trim = 0; trim < slots - 1; trim++) {
				models.add(DeconstructedStorage.getRL("block/barrel_part/" + INNER_TRIMS[trim] + trimSuffix));
			}
		}
		return models;
	}

	private static String flatPrefix(boolean flatTop) {
		return flatTop ? "flat_" : "";
	}

	/** The wood's sprite that particles take, as 1.21's barrels do theirs: the top. */
	public static TextureAtlasSprite getParticleSprite(TextureMap map, @Nullable WoodType woodType) {
		return map.getAtlasSprite(woodType == null ? BLOCK + "barrel_top_tintable_main" : BLOCK + woodType.getName() + "_barrel_top");
	}

	/**
	 * "wood" for the basic tier, whose barrels are named only "barrel" or "limited_barrel_1"; else the tier in the name, as
	 * "iron" of "iron_barrel" or "limited_iron_barrel_2".
	 */
	private static String getTierName(Block barrel) {
		String path = Objects.requireNonNull(barrel.getRegistryName()).getPath();
		if (path.startsWith("limited_")) {
			path = path.substring("limited_".length(), path.lastIndexOf('_'));
		}
		return barrel instanceof BarrelBlock && path.endsWith("_barrel") ? path.substring(0, path.length() - "_barrel".length()) : "wood";
	}

	private static List<BakedQuad> getQuads(String key, ResourceLocation model, java.util.function.Supplier<ImmutableMap<String, String>> textures,
			ModelRotation rotation, @Nullable EnumFacing side) {
		return getQuads(key, Collections.singletonList(model), textures, rotation, side);
	}

	private static List<BakedQuad> getQuads(String key, List<ResourceLocation> models, java.util.function.Supplier<ImmutableMap<String, String>> textures,
			ModelRotation rotation, @Nullable EnumFacing side) {
		Map<EnumFacing, List<BakedQuad>> bySide = BAKED.computeIfAbsent(key, k -> new EnumMap<>(ModelRotation.class))
				.computeIfAbsent(rotation, r -> bake(models, textures.get(), r));
		List<BakedQuad> quads = side == null ? bySide.get(null) : bySide.get(side);
		return quads == null ? Collections.emptyList() : quads;
	}

	/** By the face they're culled with; the unculled ones under null, which an EnumMap can't hold, so a HashMap. */
	private static Map<EnumFacing, List<BakedQuad>> bake(List<ResourceLocation> modelLocations, ImmutableMap<String, String> textures, ModelRotation rotation) {
		Map<EnumFacing, List<BakedQuad>> bySide = new HashMap<>();
		bySide.put(null, new ArrayList<>());
		for (EnumFacing side : EnumFacing.values()) {
			bySide.put(side, new ArrayList<>());
		}
		for (ResourceLocation modelLocation : modelLocations) {
			IModel model = ModelLoaderRegistry.getModelOrMissing(modelLocation).retexture(textures);
			IBakedModel baked = model.bake(rotation, DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
			bySide.get(null).addAll(baked.getQuads(null, null, 0));
			for (EnumFacing side : EnumFacing.values()) {
				bySide.get(side).addAll(baked.getQuads(null, side, 0));
			}
		}
		return bySide;
	}
}
