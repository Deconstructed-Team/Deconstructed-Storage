package dev.feldsher.deconstructedstorage.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The parts of a barrel that a block's texture can go on: its sides, bottom and top, each with its trim, and the trims
 * between a limited barrel's slots; and the groups of them that one block can cover at once, as all of the trim.
 */
public enum BarrelMaterial {
	SIDE(MaterialModelPart.CORE),
	SIDE_TRIM(MaterialModelPart.TRIM),
	BOTTOM(MaterialModelPart.CORE),
	BOTTOM_TRIM(MaterialModelPart.TRIM),
	TOP(MaterialModelPart.CORE),
	TOP_TRIM(MaterialModelPart.TRIM),
	TOP_INNER_TRIM(MaterialModelPart.TRIM),
	ALL(MaterialModelPart.BOTH, SIDE, SIDE_TRIM, BOTTOM, BOTTOM_TRIM, TOP, TOP_TRIM, TOP_INNER_TRIM),
	ALL_TRIM(MaterialModelPart.TRIM, SIDE_TRIM, BOTTOM_TRIM, TOP_TRIM, TOP_INNER_TRIM),
	ALL_BUT_TRIM(MaterialModelPart.CORE, SIDE, BOTTOM, TOP),
	TOP_ALL(MaterialModelPart.BOTH, TOP, TOP_TRIM, TOP_INNER_TRIM),
	SIDE_ALL(MaterialModelPart.BOTH, SIDE, SIDE_TRIM),
	BOTTOM_ALL(MaterialModelPart.BOTH, BOTTOM, BOTTOM_TRIM);

	private final MaterialModelPart materialModelPart;
	private final BarrelMaterial[] children;

	BarrelMaterial(MaterialModelPart materialModelPart, BarrelMaterial... children) {
		this.materialModelPart = materialModelPart;
		this.children = children;
	}

	public MaterialModelPart getMaterialModelPart() {
		return materialModelPart;
	}

	/** The name its texture goes by in the part models, and that it's saved with. */
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The parts it covers; a single part, itself. */
	public BarrelMaterial[] getChildren() {
		return children.length > 0 ? children : new BarrelMaterial[] {this};
	}

	public boolean isLeaf() {
		return children.length == 0;
	}

	public static BarrelMaterial fromName(String name) {
		for (BarrelMaterial value : values()) {
			if (value.getSerializedName().equals(name)) {
				return value;
			}
		}
		return SIDE;
	}

	/** Which parts' materials a part left without one takes, in order: a top without one is as its inner trim, or its side. */
	public static List<BarrelMaterial> getFillFromDefaults(BarrelMaterial material) {
		switch (material) {
			case SIDE:
				return Arrays.asList(BOTTOM, TOP, TOP_INNER_TRIM);
			case SIDE_TRIM:
				return Arrays.asList(BOTTOM_TRIM, TOP_TRIM);
			case BOTTOM:
				return Arrays.asList(SIDE, TOP, TOP_INNER_TRIM);
			case BOTTOM_TRIM:
				return Arrays.asList(SIDE_TRIM, TOP_TRIM);
			case TOP:
				return Arrays.asList(TOP_INNER_TRIM, SIDE, BOTTOM);
			case TOP_TRIM:
				return Arrays.asList(SIDE_TRIM, BOTTOM_TRIM);
			case TOP_INNER_TRIM:
				return Arrays.asList(TOP_TRIM, SIDE_TRIM, BOTTOM_TRIM);
			default:
				return Collections.emptyList();
		}
	}

	/** Which of the barrel's split models the part is in: its core, its trim, or both. */
	public enum MaterialModelPart {
		BOTH,
		TRIM,
		CORE
	}
}
