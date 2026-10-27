package dev.feldsher.deconstructedstorage.block;

import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * What a barrel's model is made of, besides its facing: its wood, or none when it's tinted all over, which colors it
 * has, whether it's open, and whether it's packed. Equal data makes the same quads, which the model keeps.
 */
public final class BarrelRenderData {
	/** A barrel with no block entity to say, as one an item shows: acacia, closed. */
	public static final BarrelRenderData DEFAULT = new BarrelRenderData(WoodType.DEFAULT, false, false, false, false);

	@Nullable
	private final WoodType woodType;
	private final boolean hasMainColor;
	private final boolean hasAccentColor;
	private final boolean open;
	private final boolean packed;

	public BarrelRenderData(@Nullable WoodType woodType, boolean hasMainColor, boolean hasAccentColor, boolean open, boolean packed) {
		this.woodType = woodType;
		this.hasMainColor = hasMainColor;
		this.hasAccentColor = hasAccentColor;
		this.open = open;
		this.packed = packed;
	}

	public static BarrelRenderData of(WoodStorageBlockEntity be) {
		StorageWrapper wrapper = be.getStorageWrapper();
		return new BarrelRenderData(be.getWoodType().orElse(null), wrapper.hasMainColor(), wrapper.hasAccentColor(), be.isOpen(), be.isPacked());
	}

	@Nullable
	public WoodType getWoodType() {
		return woodType;
	}

	public boolean hasMainColor() {
		return hasMainColor;
	}

	public boolean hasAccentColor() {
		return hasAccentColor;
	}

	public boolean isOpen() {
		return open;
	}

	public boolean isPacked() {
		return packed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BarrelRenderData)) {
			return false;
		}
		BarrelRenderData that = (BarrelRenderData) o;
		return hasMainColor == that.hasMainColor && hasAccentColor == that.hasAccentColor && open == that.open && packed == that.packed && woodType == that.woodType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(woodType, hasMainColor, hasAccentColor, open, packed);
	}

	static class Property implements IUnlistedProperty<BarrelRenderData> {
		@Override
		public String getName() {
			return "render_data";
		}

		@Override
		public boolean isValid(BarrelRenderData value) {
			return value != null;
		}

		@Override
		public Class<BarrelRenderData> getType() {
			return BarrelRenderData.class;
		}

		@Override
		public String valueToString(BarrelRenderData value) {
			return value.toString();
		}
	}
}
