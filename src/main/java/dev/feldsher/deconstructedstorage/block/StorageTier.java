package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.Config;

import java.util.Locale;

/** The tiers storages come in, from the wooden ones up. Copper and netherite are there only as the config allows. */
public enum StorageTier {
	BASIC, COPPER, IRON, GOLD, DIAMOND, NETHERITE;

	public boolean isEnabled() {
		return this == COPPER ? Config.SERVER.copperTierEnabled : this != NETHERITE || Config.SERVER.netheriteTierEnabled;
	}

	public String getName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
