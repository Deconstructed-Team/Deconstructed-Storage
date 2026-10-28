package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A barrel with full blocks around it comes out made of them, by where they are: above it, its top, the one right
 * above its top's inner trim, and the one to the right its top trim; beside it on the left its side, on the right its
 * side trim; below it, its bottom and bottom trim. Parts left without a block take those of the parts next to them.
 */
public class BarrelMaterialRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
	@Override
	public boolean matches(InventoryCrafting inv, World world) {
		int barrelRow = -1;
		int barrelCol = -1;
		int minRowWithBlock = Integer.MAX_VALUE;
		int minColWithBlock = Integer.MAX_VALUE;
		int maxRowWithBlock = Integer.MIN_VALUE;
		int maxColWithBlock = Integer.MIN_VALUE;
		Map<Integer, Integer> rowCounts = new HashMap<>();

		for (int row = 0; row < inv.getHeight(); row++) {
			for (int col = 0; col < inv.getWidth(); col++) {
				ItemStack item = inv.getStackInSlot(col + row * inv.getWidth());
				if (item.isEmpty()) {
					continue;
				}
				if (isBarrel(item)) {
					if (barrelRow >= 0) {
						return false;
					}
					barrelRow = row;
					barrelCol = col;
				} else if (isFullBlock(item)) {
					boolean isBottomMiddleAndBottomLeftHasBlock = barrelCol == col && barrelRow < row && rowCounts.getOrDefault(row, 0) > 0;
					if (isBottomMiddleAndBottomLeftHasBlock) {
						return false;
					}
					rowCounts.merge(row, 1, Integer::sum);
					minRowWithBlock = Math.min(minRowWithBlock, row);
					minColWithBlock = Math.min(minColWithBlock, col);
					maxRowWithBlock = Math.max(maxRowWithBlock, row);
					maxColWithBlock = Math.max(maxColWithBlock, col);
				} else {
					return false;
				}
			}
		}

		if (barrelRow < 0 || rowCounts.isEmpty() || minRowWithBlock < barrelRow - 1 || maxRowWithBlock > barrelRow + 1 || minColWithBlock < barrelCol - 1
				|| maxColWithBlock > barrelCol + 1) {
			return false;
		}
		return rowCounts.getOrDefault(barrelRow - 1, 0) <= 3 && rowCounts.getOrDefault(barrelRow, 0) <= 2 && rowCounts.getOrDefault(barrelRow + 1, 0) <= 2;
	}

	private static boolean isBarrel(ItemStack stack) {
		return stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() instanceof BarrelBlock;
	}

	@SuppressWarnings("deprecation")
	private static boolean isFullBlock(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemBlock)) {
			return false;
		}
		Block block = ((ItemBlock) stack.getItem()).getBlock();
		try {
			return block.getStateFromMeta(stack.getItem().getMetadata(stack.getMetadata())).isFullCube();
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		int barrelColumn = -1;
		int barrelRow = -1;
		ItemStack barrelStackCopy = ItemStack.EMPTY;
		for (int row = 0; row < inv.getHeight() && barrelColumn < 0; row++) {
			for (int col = 0; col < inv.getWidth(); col++) {
				ItemStack item = inv.getStackInSlot(col + row * inv.getWidth());
				if (isBarrel(item)) {
					barrelColumn = col;
					barrelRow = row;
					barrelStackCopy = item.copy();
					barrelStackCopy.setCount(1);
					break;
				}
			}
		}
		if (barrelStackCopy.isEmpty()) {
			return ItemStack.EMPTY;
		}

		Map<BarrelMaterial, MaterialBlock> materials = new EnumMap<>(BarrelMaterial.class);
		materials.putAll(BarrelBlockItem.getMaterials(barrelStackCopy));
		BarrelBlockItem.uncompactMaterials(materials);
		fillGridMaterials(inv, barrelColumn, barrelRow, materials);
		fillEmptyMaterialsWithDefaults(materials);
		BarrelBlockItem.compactMaterials(materials);
		BarrelBlockItem.setMaterials(barrelStackCopy, materials);
		BarrelBlockItem.removeCoveredTints(barrelStackCopy, materials);
		return barrelStackCopy;
	}

	private static void fillEmptyMaterialsWithDefaults(Map<BarrelMaterial, MaterialBlock> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (material.isLeaf() && !materials.containsKey(material)) {
				for (BarrelMaterial fillFromDefault : BarrelMaterial.getFillFromDefaults(material)) {
					if (materials.containsKey(fillFromDefault)) {
						materials.put(material, materials.get(fillFromDefault));
						break;
					}
				}
			}
		}
	}

	private static void fillGridMaterials(InventoryCrafting inv, int barrelColumn, int barrelRow, Map<BarrelMaterial, MaterialBlock> materials) {
		for (int row = 0; row < inv.getHeight(); row++) {
			for (int col = 0; col < inv.getWidth(); col++) {
				ItemStack item = inv.getStackInSlot(col + row * inv.getWidth());
				if (row == barrelRow && col == barrelColumn || item.isEmpty()) {
					continue;
				}
				int itemRow = row;
				int itemCol = col;
				MaterialBlock.of(item).ifPresent(block -> {
					boolean firstMaterial = true;
					for (BarrelMaterial barrelMaterial : getBarrelMaterials(itemRow, itemCol, barrelRow, barrelColumn)) {
						if (!materials.containsKey(barrelMaterial) || firstMaterial) {
							materials.put(barrelMaterial, block);
						}
						firstMaterial = false;
					}
				});
			}
		}
	}

	private static List<BarrelMaterial> getBarrelMaterials(int row, int col, int barrelRow, int barrelColumn) {
		if (row < barrelRow) {
			if (col < barrelColumn) {
				return Collections.singletonList(BarrelMaterial.TOP);
			} else if (col == barrelColumn) {
				return Arrays.asList(BarrelMaterial.TOP_INNER_TRIM, BarrelMaterial.TOP, BarrelMaterial.TOP_TRIM);
			}
			return Collections.singletonList(BarrelMaterial.TOP_TRIM);
		} else if (row == barrelRow) {
			return Collections.singletonList(col < barrelColumn ? BarrelMaterial.SIDE : BarrelMaterial.SIDE_TRIM);
		}
		if (col < barrelColumn) {
			return Collections.singletonList(BarrelMaterial.BOTTOM);
		} else if (col == barrelColumn) {
			return Arrays.asList(BarrelMaterial.BOTTOM, BarrelMaterial.BOTTOM_TRIM);
		}
		return Collections.singletonList(BarrelMaterial.BOTTOM_TRIM);
	}

	@Override
	public boolean canFit(int width, int height) {
		return width * height > 1;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	/** {@code "type": "deconstructedstorage:barrel_material"}, nothing else. */
	public static class Factory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			return new BarrelMaterialRecipe();
		}
	}
}
