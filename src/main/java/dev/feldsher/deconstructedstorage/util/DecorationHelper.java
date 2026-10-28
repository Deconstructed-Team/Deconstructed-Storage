package dev.feldsher.deconstructedstorage.util;

import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * What decorating costs: a block makes 24 parts, of which each part of a barrel takes some, its sides most; a color
 * takes red, green and blue dye parts as it's made of them, a storage's main color 18 and its accent 6. What's left of
 * a block or dye is kept for later, by the material's or dye's key: "minecraft:wool@14", or "dyeRed".
 */
public final class DecorationHelper {
	public static final int BLOCK_TOTAL_PARTS = 24;
	public static final String RED_DYE = "dyeRed";
	public static final String GREEN_DYE = "dyeGreen";
	public static final String BLUE_DYE = "dyeBlue";
	private static final int MAIN_COLOR_PARTS = 18;
	private static final int ACCENT_COLOR_PARTS = 6;
	private static final Map<BarrelMaterial, Integer> DECORATIVE_SLOT_PARTS_NEEDED = new EnumMap<>(BarrelMaterial.class);

	static {
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.TOP_INNER_TRIM, 1);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.TOP_TRIM, 1);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.SIDE_TRIM, 4);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.BOTTOM_TRIM, 1);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.TOP, 3);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.SIDE, 12);
		DECORATIVE_SLOT_PARTS_NEEDED.put(BarrelMaterial.BOTTOM, 3);
	}

	private DecorationHelper() {}

	public static boolean isDye(ItemStack stack, String dyeName) {
		if (stack.isEmpty()) {
			return false;
		}
		for (int oreId : OreDictionary.getOreIDs(stack)) {
			if (OreDictionary.getOreName(oreId).equals(dyeName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean consumeDyes(int mainColorBeingSet, int accentColorBeingSet, Map<String, Integer> remainingParts, List<IItemHandler> dyes, int storageMainColor,
			int storageAccentColor, boolean simulate) {
		Map<String, Integer> partsNeeded = getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor);
		return partsNeeded.isEmpty() || consumePartsNeeded(partsNeeded, dyes, (dyeName, stack) -> isDye(stack, dyeName), remainingParts, simulate).hasEnough();
	}

	public static Map<String, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor) {
		return getDyePartsNeeded(mainColorBeingSet, accentColorBeingSet, storageMainColor, storageAccentColor, MAIN_COLOR_PARTS, ACCENT_COLOR_PARTS);
	}

	public static Map<String, Integer> getDyePartsNeeded(int mainColorBeingSet, int accentColorBeingSet, int storageMainColor, int storageAccentColor, int mainColorParts,
			int accentColorParts) {
		Map<String, Integer> partsNeeded = new HashMap<>();
		if (mainColorBeingSet != -1 && mainColorBeingSet != storageMainColor) {
			addPartsNeededIfAny(calculateRGBPartsNeeded(mainColorBeingSet, mainColorParts), partsNeeded);
		}
		if (accentColorBeingSet != -1 && accentColorBeingSet != storageAccentColor) {
			addPartsNeededIfAny(calculateRGBPartsNeeded(accentColorBeingSet, accentColorParts), partsNeeded);
		}
		return partsNeeded;
	}

	private static void addPartsNeededIfAny(int[] rgbPartsNeeded, Map<String, Integer> partsNeeded) {
		addPartsNeededIfAny(rgbPartsNeeded[0], partsNeeded, RED_DYE);
		addPartsNeededIfAny(rgbPartsNeeded[1], partsNeeded, GREEN_DYE);
		addPartsNeededIfAny(rgbPartsNeeded[2], partsNeeded, BLUE_DYE);
	}

	private static void addPartsNeededIfAny(int parts, Map<String, Integer> partsNeeded, String dyeName) {
		if (parts != 0) {
			partsNeeded.merge(dyeName, parts, Integer::sum);
		}
	}

	/** The parts split as the color's red, green and blue are, the remainders going to the largest fractions. */
	private static int[] calculateRGBPartsNeeded(int color, int totalParts) {
		float[] ratios = {(color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f};
		float totalRatios = ratios[0] + ratios[1] + ratios[2];
		if (totalRatios == 0) {
			// black: as much of each
			ratios = new float[] {1, 1, 1};
			totalRatios = 3;
		}
		int n = ratios.length;
		int[] result = new int[n];
		double[] remainders = new double[n];
		for (int i = 0; i < n; i++) {
			double scaled = ratios[i] / totalRatios * totalParts;
			result[i] = (int) scaled;
			remainders[i] = scaled - result[i];
		}
		int remaining = totalParts - Arrays.stream(result).sum();
		Integer[] indices = {0, 1, 2};
		Arrays.sort(indices, Comparator.comparingDouble(i -> -remainders[i]));
		for (int i = 0; i < remaining; i++) {
			result[indices[i % n]]++;
		}
		return result;
	}

	public static boolean consumeMaterials(Map<String, Integer> remainingParts, List<IItemHandler> decorativeBlocks, Map<BarrelMaterial, MaterialBlock> originalMaterials,
			Map<BarrelMaterial, MaterialBlock> materials, boolean simulate) {
		Map<String, Integer> partsNeeded = getMaterialPartsNeeded(originalMaterials, materials);
		return consumePartsNeeded(partsNeeded, decorativeBlocks, (key, stack) -> MaterialBlock.of(stack).map(block -> block.toString().equals(key)).orElse(false),
				remainingParts, simulate).hasEnough();
	}

	/** What applying these materials takes, of each block: the parts that change, a top trim the same as its inner trim for free. */
	public static Map<String, Integer> getMaterialPartsNeeded(Map<BarrelMaterial, MaterialBlock> originalMaterials, Map<BarrelMaterial, MaterialBlock> materialsToApply) {
		Map<String, Integer> partsNeeded = new HashMap<>();
		Map<BarrelMaterial, MaterialBlock> materials = new EnumMap<>(BarrelMaterial.class);
		materials.putAll(materialsToApply);
		BarrelBlockItem.uncompactMaterials(materials);

		MaterialBlock topInnerTrim = addMaterialCost(materials, BarrelMaterial.TOP_INNER_TRIM, null, partsNeeded, originalMaterials);
		MaterialBlock topTrim = addMaterialCost(materials, BarrelMaterial.TOP_TRIM, topInnerTrim, partsNeeded, originalMaterials);
		MaterialBlock sideTrim = addMaterialCost(materials, BarrelMaterial.SIDE_TRIM, topTrim, partsNeeded, originalMaterials);
		addMaterialCost(materials, BarrelMaterial.BOTTOM_TRIM, sideTrim, partsNeeded, originalMaterials);
		MaterialBlock top = addMaterialCost(materials, BarrelMaterial.TOP, topTrim, partsNeeded, originalMaterials);
		MaterialBlock side = addMaterialCost(materials, BarrelMaterial.SIDE, top, partsNeeded, originalMaterials);
		addMaterialCost(materials, BarrelMaterial.BOTTOM, side, partsNeeded, originalMaterials);
		return partsNeeded;
	}

	@Nullable
	private static MaterialBlock addMaterialCost(Map<BarrelMaterial, MaterialBlock> materials, BarrelMaterial barrelMaterial, @Nullable MaterialBlock defaultMaterial,
			Map<String, Integer> partsNeeded, Map<BarrelMaterial, MaterialBlock> originalMaterials) {
		boolean materialIsTheSame = Objects.equals(originalMaterials.get(barrelMaterial), materials.get(barrelMaterial));
		boolean newHasNoMaterial = !materials.containsKey(barrelMaterial);
		boolean hasNoCost = barrelMaterial == BarrelMaterial.TOP_TRIM && defaultMaterial != null || materialIsTheSame || newHasNoMaterial;
		MaterialBlock material = materials.getOrDefault(barrelMaterial, defaultMaterial);
		if (!hasNoCost && material != null) {
			partsNeeded.merge(material.toString(), DECORATIVE_SLOT_PARTS_NEEDED.get(barrelMaterial), Integer::sum);
		}
		return material;
	}

	/** Takes what's needed from what's left of earlier blocks and dyes first, then from the handlers, a whole block or dye at a time. */
	public static ConsumptionResult consumePartsNeeded(Map<String, Integer> partsNeeded, List<IItemHandler> resourceHandlers, BiPredicate<String, ItemStack> stackMatcher,
			Map<String, Integer> remainingParts, boolean simulate) {
		Map<String, Integer> missingParts = new HashMap<>();
		for (Map.Entry<String, Integer> entry : partsNeeded.entrySet()) {
			String key = entry.getKey();
			int parts = entry.getValue();
			int remainingPartCount = remainingParts.getOrDefault(key, 0);
			if (remainingPartCount > parts) {
				if (!simulate) {
					remainingParts.put(key, remainingPartCount - parts);
				}
				continue;
			}
			if (!simulate) {
				remainingParts.remove(key);
			}
			if (remainingPartCount == parts) {
				continue;
			}
			int missing = consumeFromHandlers(resourceHandlers, stackMatcher, remainingParts, simulate, key, parts - remainingPartCount);
			if (missing > 0) {
				missingParts.put(key, missing);
			}
		}
		return new ConsumptionResult(missingParts.isEmpty(), missingParts);
	}

	private static int consumeFromHandlers(List<IItemHandler> resourceHandlers, BiPredicate<String, ItemStack> stackMatcher, Map<String, Integer> remainingParts,
			boolean simulate, String key, int parts) {
		for (IItemHandler resources : resourceHandlers) {
			for (int slot = 0; slot < resources.getSlots(); slot++) {
				ItemStack stack = resources.getStackInSlot(slot);
				if (!stackMatcher.test(key, stack)) {
					continue;
				}
				int toRemove = (parts + BLOCK_TOTAL_PARTS - 1) / BLOCK_TOTAL_PARTS;
				ItemStack removed = resources.extractItem(slot, toRemove, simulate);
				int partsRemoved = removed.getCount() * BLOCK_TOTAL_PARTS;
				if (partsRemoved >= parts) {
					if (partsRemoved > parts && !simulate) {
						remainingParts.put(key, partsRemoved - parts);
					}
					return 0;
				}
				parts -= partsRemoved;
			}
		}
		return parts;
	}

	public static final class ConsumptionResult {
		private final boolean hasEnough;
		private final Map<String, Integer> missingParts;

		ConsumptionResult(boolean hasEnough, Map<String, Integer> missingParts) {
			this.hasEnough = hasEnough;
			this.missingParts = missingParts;
		}

		public boolean hasEnough() {
			return hasEnough;
		}

		public Map<String, Integer> getMissingParts() {
			return missingParts;
		}
	}
}
