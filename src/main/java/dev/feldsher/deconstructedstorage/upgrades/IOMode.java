package dev.feldsher.deconstructedstorage.upgrades;

import java.util.Locale;

/** What an upgrade does on a side: pushes items out, pulls them in, both or nothing. */
public enum IOMode {
	PUSH,
	PULL,
	PUSH_PULL,
	OFF,
	DISABLED;

	private static final IOMode[] VALUES = values();

	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public IOMode next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public static IOMode fromName(String name) {
		for (IOMode value : VALUES) {
			if (value.getSerializedName().equals(name)) {
				return value;
			}
		}
		return OFF;
	}
}
