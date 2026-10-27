package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
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

	public static final ChestBlock CHEST = block(new ChestBlock(() -> Config.SERVER.woodChest.inventorySlotCount, () -> Config.SERVER.woodChest.upgradeSlotCount, true), "chest");
	public static final ChestBlock COPPER_CHEST = block(new ChestBlock(() -> Config.SERVER.copperChest.inventorySlotCount, () -> Config.SERVER.copperChest.upgradeSlotCount, false), "copper_chest");
	public static final ChestBlock IRON_CHEST = block(new ChestBlock(() -> Config.SERVER.ironChest.inventorySlotCount, () -> Config.SERVER.ironChest.upgradeSlotCount, false), "iron_chest");
	public static final ChestBlock GOLD_CHEST = block(new ChestBlock(() -> Config.SERVER.goldChest.inventorySlotCount, () -> Config.SERVER.goldChest.upgradeSlotCount, false), "gold_chest");
	public static final ChestBlock DIAMOND_CHEST = block(new ChestBlock(() -> Config.SERVER.diamondChest.inventorySlotCount, () -> Config.SERVER.diamondChest.upgradeSlotCount, false), "diamond_chest");
	public static final ChestBlock NETHERITE_CHEST = block(new ChestBlock(() -> Config.SERVER.netheriteChest.inventorySlotCount, () -> Config.SERVER.netheriteChest.upgradeSlotCount, false), "netherite_chest");

	static {
		// as 1.21's netherite storages, 1200: 1.12 keeps three times what it's given
		NETHERITE_BARREL.setResistance(400);
		NETHERITE_CHEST.setResistance(400);
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

	/** The chest tiers the config enables. */
	public static List<ChestBlock> getChests() {
		List<ChestBlock> chests = new ArrayList<>(Arrays.asList(CHEST, IRON_CHEST, GOLD_CHEST, DIAMOND_CHEST));
		if (Config.SERVER.copperTierEnabled) {
			chests.add(1, COPPER_CHEST);
		}
		if (Config.SERVER.netheriteTierEnabled) {
			chests.add(NETHERITE_CHEST);
		}
		return chests;
	}

	public static List<Block> getAllBlocks() {
		List<Block> blocks = new ArrayList<>(getBarrels());
		blocks.addAll(getChests());
		return blocks;
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
		GameRegistry.registerTileEntity(ChestBlockEntity.class, DeconstructedStorage.getRL("chest"));
	}
}
