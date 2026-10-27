package dev.feldsher.deconstructedstorage.block;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nullable;
import java.util.Locale;

/** The woods a wooden storage is made of: 1.12's six. Kept by name on the block and its stack, as 1.21's wood types. */
public enum WoodType implements IStringSerializable {
	OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK;

	/** What a storage is made of when nothing says: as 1.21, acacia. */
	public static final WoodType DEFAULT = ACACIA;

	@Override
	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}

	@Nullable
	public static WoodType fromName(String name) {
		for (WoodType woodType : values()) {
			if (woodType.getName().equals(name)) {
				return woodType;
			}
		}
		return null;
	}
}
