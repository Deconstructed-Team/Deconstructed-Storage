package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedstorage.block.VerticalFacing;
import net.minecraft.util.EnumFacing;

import java.util.Locale;

/** A side of a storage as seen from its front, which is a direction in the world by how the storage is turned. */
public enum BlockSide {
	TOP,
	BOTTOM,
	FRONT,
	BACK,
	LEFT,
	RIGHT;

	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static BlockSide fromName(String name) {
		for (BlockSide value : values()) {
			if (value.getSerializedName().equals(name)) {
				return value;
			}
		}
		return FRONT;
	}

	/** Which side of a block facing that way the direction is. */
	public static BlockSide fromDirection(EnumFacing direction, EnumFacing baseHorizontalDirection, VerticalFacing baseVerticalFacing) {
		for (BlockSide side : values()) {
			if (side.toDirection(baseHorizontalDirection, baseVerticalFacing) == direction) {
				return side;
			}
		}
		return FRONT;
	}

	public EnumFacing toDirection(EnumFacing baseHorizontalDirection, VerticalFacing baseVerticalFacing) {
		switch (this) {
			case TOP:
				if (baseVerticalFacing == VerticalFacing.NO) {
					return EnumFacing.UP;
				}
				return baseVerticalFacing == VerticalFacing.UP ? baseHorizontalDirection.getOpposite() : baseHorizontalDirection;
			case BOTTOM:
				if (baseVerticalFacing == VerticalFacing.NO) {
					return EnumFacing.DOWN;
				}
				return baseVerticalFacing == VerticalFacing.UP ? baseHorizontalDirection : baseHorizontalDirection.getOpposite();
			case FRONT:
				return baseVerticalFacing == VerticalFacing.NO ? baseHorizontalDirection : baseVerticalFacing.getDirection();
			case BACK:
				return baseVerticalFacing == VerticalFacing.NO ? baseHorizontalDirection.getOpposite() : baseVerticalFacing.getDirection().getOpposite();
			case LEFT:
				return baseHorizontalDirection.rotateY();
			case RIGHT:
			default:
				return baseHorizontalDirection.rotateYCCW();
		}
	}
}
