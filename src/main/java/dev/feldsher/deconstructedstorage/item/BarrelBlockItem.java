package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** A barrel as an item: also whether its top is flat, and which blocks' textures it's made of. */
public class BarrelBlockItem extends WoodStorageBlockItem {
	private static final String FLAT_TOP_TAG = "flat_top";
	private static final String MATERIALS_TAG = "materials";

	public BarrelBlockItem(Block block) {
		super(block);
	}

	public static boolean isFlatTop(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return stack.getItem() instanceof BarrelBlockItem && tag != null && tag.getBoolean(FLAT_TOP_TAG);
	}

	public static void setFlatTop(ItemStack stack, boolean flatTop) {
		if (flatTop) {
			StorageNbt.getOrCreateTag(stack).setBoolean(FLAT_TOP_TAG, true);
		} else if (stack.getTagCompound() != null) {
			stack.getTagCompound().removeTag(FLAT_TOP_TAG);
		}
	}

	public static Map<BarrelMaterial, MaterialBlock> getMaterials(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag == null ? Collections.emptyMap() : readMaterials(tag.getCompoundTag(MATERIALS_TAG));
	}

	public static void setMaterials(ItemStack stack, Map<BarrelMaterial, MaterialBlock> materials) {
		if (materials.isEmpty()) {
			removeMaterials(stack);
		} else {
			StorageNbt.getOrCreateTag(stack).setTag(MATERIALS_TAG, writeMaterials(materials));
		}
	}

	public static void removeMaterials(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(MATERIALS_TAG);
		}
	}

	public static NBTTagCompound writeMaterials(Map<BarrelMaterial, MaterialBlock> materials) {
		NBTTagCompound tag = new NBTTagCompound();
		materials.forEach((material, block) -> tag.setString(material.getSerializedName(), block.toString()));
		return tag;
	}

	public static Map<BarrelMaterial, MaterialBlock> readMaterials(NBTTagCompound tag) {
		if (tag.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<BarrelMaterial, MaterialBlock> materials = new EnumMap<>(BarrelMaterial.class);
		for (String key : tag.getKeySet()) {
			MaterialBlock block = MaterialBlock.parse(tag.getString(key));
			if (block != null) {
				materials.put(BarrelMaterial.fromName(key), block);
			}
		}
		return materials;
	}

	/** Each group of parts into the parts it covers. */
	public static void uncompactMaterials(Map<BarrelMaterial, MaterialBlock> materials) {
		if (materials.isEmpty()) {
			return;
		}
		Map<BarrelMaterial, MaterialBlock> uncompactedMaterials = new EnumMap<>(BarrelMaterial.class);
		materials.forEach((material, block) -> {
			for (BarrelMaterial child : material.getChildren()) {
				uncompactedMaterials.put(child, block);
			}
		});
		materials.clear();
		materials.putAll(uncompactedMaterials);
	}

	/** Parts all of one group that have the same block, as the group. */
	public static void compactMaterials(Map<BarrelMaterial, MaterialBlock> materials) {
		for (BarrelMaterial material : BarrelMaterial.values()) {
			if (material.isLeaf()) {
				continue;
			}
			MaterialBlock firstChildBlock = null;
			boolean allChildrenHaveSameBlock = true;
			for (BarrelMaterial child : material.getChildren()) {
				MaterialBlock block = materials.get(child);
				if (block == null || firstChildBlock != null && !firstChildBlock.equals(block)) {
					allChildrenHaveSameBlock = false;
					break;
				} else if (firstChildBlock == null) {
					firstChildBlock = block;
				}
			}
			if (allChildrenHaveSameBlock && firstChildBlock != null) {
				materials.put(material, firstChildBlock);
				for (BarrelMaterial child : material.getChildren()) {
					materials.remove(child);
				}
			}
		}
	}

	/** A tint the materials cover all of goes: the main one under all but the trim, the accent under all the trim. */
	public static void removeCoveredTints(ItemStack stack, Map<BarrelMaterial, MaterialBlock> materials) {
		if (StorageBlockItem.getMainColor(stack).isPresent() && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_BUT_TRIM))) {
			StorageBlockItem.removeMainColor(stack);
		}
		if (StorageBlockItem.getAccentColor(stack).isPresent() && (materials.containsKey(BarrelMaterial.ALL) || materials.containsKey(BarrelMaterial.ALL_TRIM))) {
			StorageBlockItem.removeAccentColor(stack);
		}
	}
}
