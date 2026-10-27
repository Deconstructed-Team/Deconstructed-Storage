package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public class ModBlocks {
	private ModBlocks() {}

	public static final BarrelBlock BARREL = block(new BarrelBlock(() -> Config.SERVER.woodBarrel.inventorySlotCount, () -> Config.SERVER.woodBarrel.upgradeSlotCount, true), "barrel");
	public static final BarrelBlock COPPER_BARREL = block(new BarrelBlock(() -> Config.SERVER.copperBarrel.inventorySlotCount, () -> Config.SERVER.copperBarrel.upgradeSlotCount, false), "copper_barrel");
	public static final BarrelBlock IRON_BARREL = block(new BarrelBlock(() -> Config.SERVER.ironBarrel.inventorySlotCount, () -> Config.SERVER.ironBarrel.upgradeSlotCount, false), "iron_barrel");
	public static final BarrelBlock GOLD_BARREL = block(new BarrelBlock(() -> Config.SERVER.goldBarrel.inventorySlotCount, () -> Config.SERVER.goldBarrel.upgradeSlotCount, false), "gold_barrel");
	public static final BarrelBlock DIAMOND_BARREL = block(new BarrelBlock(() -> Config.SERVER.diamondBarrel.inventorySlotCount, () -> Config.SERVER.diamondBarrel.upgradeSlotCount, false), "diamond_barrel");
	public static final BarrelBlock NETHERITE_BARREL = block(new BarrelBlock(() -> Config.SERVER.netheriteBarrel.inventorySlotCount, () -> Config.SERVER.netheriteBarrel.upgradeSlotCount, false), "netherite_barrel");

	static {
		// as 1.21's netherite storages, 1200: 1.12 keeps three times what it's given
		NETHERITE_BARREL.setResistance(400);
	}

	/** The barrel tiers the config enables. */
	public static List<BarrelBlock> getBarrels() {
		List<BarrelBlock> barrels = new ArrayList<>(Arrays.asList(BARREL, IRON_BARREL, GOLD_BARREL, DIAMOND_BARREL));
		if (Config.SERVER.copperTierEnabled) {
			barrels.add(1, COPPER_BARREL);
		}
		if (Config.SERVER.netheriteTierEnabled) {
			barrels.add(NETHERITE_BARREL);
		}
		return barrels;
	}

	public static List<Block> getAllBlocks() {
		return new ArrayList<>(getBarrels());
	}

	private static <T extends Block> T block(T block, String name) {
		block.setRegistryName(DeconstructedStorage.getRL(name));
		block.setTranslationKey(DeconstructedStorage.MOD_ID + "." + name);
		block.setCreativeTab(ModItems.CREATIVE_TAB);
		return block;
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		getAllBlocks().forEach(event.getRegistry()::register);
		GameRegistry.registerTileEntity(BarrelBlockEntity.class, DeconstructedStorage.getRL("barrel"));
	}
}
