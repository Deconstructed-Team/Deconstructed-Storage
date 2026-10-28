package dev.feldsher.deconstructedstorage.block;

import java.util.Map;

/** A storage made of other blocks' textures, as a barrel. */
public interface IMaterialHolder {
	void setMaterials(Map<BarrelMaterial, MaterialBlock> materials);

	Map<BarrelMaterial, MaterialBlock> getMaterials();

	boolean canHoldMaterials();
}
