package dev.feldsher.deconstructedstorage.compat.quark;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.TierUpgrader;
import dev.feldsher.deconstructedstorage.block.WoodType;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * With Quark there: its chests of other woods go up a tier as vanilla's chest does, of their wood. Quark's chest is
 * vanilla's chest with the wood in its block entity's {@code chestType}, read by reflection, as Quark has no API for it.
 */
public final class QuarkCompat {
	public static final String MOD_ID = "quark";

	private QuarkCompat() {}

	public static void init() {
		Block customChest = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(MOD_ID, "custom_chest"));
		if (customChest == null || customChest == Blocks.AIR) {
			return;
		}
		Field chestTypeField;
		try {
			chestTypeField = Class.forName("vazkii.quark.decoration.tile.TileCustomChest").getField("chestType");
		} catch (ReflectiveOperationException e) {
			DeconstructedStorage.LOGGER.warn("Quark's chests are not what was expected, they won't go up a tier", e);
			return;
		}
		TierUpgrader.registerVanillaChest(customChest, te -> getWoodType(chestTypeField, te));
	}

	/** The wood named as Quark's chest type is, oak for its none. */
	private static WoodType getWoodType(Field chestTypeField, TileEntity te) {
		if (!chestTypeField.getDeclaringClass().isInstance(te)) {
			return WoodType.OAK;
		}
		try {
			Object chestType = chestTypeField.get(te);
			WoodType woodType = chestType instanceof Enum ? WoodType.fromName(((Enum<?>) chestType).name().toLowerCase(Locale.ROOT)) : null;
			return woodType == null ? WoodType.OAK : woodType;
		} catch (IllegalAccessException e) {
			return WoodType.OAK;
		}
	}
}
