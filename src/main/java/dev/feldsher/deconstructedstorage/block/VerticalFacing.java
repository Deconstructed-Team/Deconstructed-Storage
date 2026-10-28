package dev.feldsher.deconstructedstorage.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/** Whether a storage's front looks up or down, or to a side. */
public enum VerticalFacing implements IStringSerializable {
	NO(EnumFacing.NORTH),
	UP(EnumFacing.UP),
	DOWN(EnumFacing.DOWN);

	private final EnumFacing direction;

	VerticalFacing(EnumFacing direction) {
		this.direction = direction;
	}

	public EnumFacing getDirection() {
		return direction;
	}

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static VerticalFacing fromDirection(EnumFacing direction) {
		if (direction == EnumFacing.UP) {
			return UP;
		} else if (direction == EnumFacing.DOWN) {
			return DOWN;
		}
		return NO;
	}
}
