package dev.feldsher.deconstructedstorage.block;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;

import java.util.Locale;

/**
 * A chest alone, or the half of a double chest it is, as 1.21's chest types: looking at its front, the left half has
 * the other to its right.
 */
public enum ChestType implements IStringSerializable {
	SINGLE, LEFT, RIGHT;

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public ChestType getOpposite() {
		return this == LEFT ? RIGHT : this == RIGHT ? LEFT : SINGLE;
	}

	/** Where the other half is, from a chest of this type facing that way. */
	public EnumFacing getConnectedDirection(EnumFacing facing) {
		return this == LEFT ? facing.rotateY() : facing.rotateYCCW();
	}
}
