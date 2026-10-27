package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlock;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageTier;
import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

	public static final ShulkerBoxBlock SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.shulkerBox.inventorySlotCount, () -> Config.SERVER.shulkerBox.upgradeSlotCount, true), "shulker_box");
	public static final ShulkerBoxBlock COPPER_SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.copperShulkerBox.inventorySlotCount, () -> Config.SERVER.copperShulkerBox.upgradeSlotCount, false), "copper_shulker_box");
	public static final ShulkerBoxBlock IRON_SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.ironShulkerBox.inventorySlotCount, () -> Config.SERVER.ironShulkerBox.upgradeSlotCount, false), "iron_shulker_box");
	public static final ShulkerBoxBlock GOLD_SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.goldShulkerBox.inventorySlotCount, () -> Config.SERVER.goldShulkerBox.upgradeSlotCount, false), "gold_shulker_box");
	public static final ShulkerBoxBlock DIAMOND_SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.diamondShulkerBox.inventorySlotCount, () -> Config.SERVER.diamondShulkerBox.upgradeSlotCount, false), "diamond_shulker_box");
	public static final ShulkerBoxBlock NETHERITE_SHULKER_BOX = block(new ShulkerBoxBlock(() -> Config.SERVER.netheriteShulkerBox.inventorySlotCount, () -> Config.SERVER.netheriteShulkerBox.upgradeSlotCount, false), "netherite_shulker_box");

	static {
		// as 1.21's netherite storages, 1200: 1.12 keeps three times what it's given
		NETHERITE_BARREL.setResistance(400);
		NETHERITE_CHEST.setResistance(400);
		NETHERITE_SHULKER_BOX.setResistance(400);
	}

	private static final Block[][] TIERED = {
			{BARREL, COPPER_BARREL, IRON_BARREL, GOLD_BARREL, DIAMOND_BARREL, NETHERITE_BARREL},
			{CHEST, COPPER_CHEST, IRON_CHEST, GOLD_CHEST, DIAMOND_CHEST, NETHERITE_CHEST},
			{SHULKER_BOX, COPPER_SHULKER_BOX, IRON_SHULKER_BOX, GOLD_SHULKER_BOX, DIAMOND_SHULKER_BOX, NETHERITE_SHULKER_BOX}
	};

	/** The storage's tier, when it's one of these storages. */
	public static Optional<StorageTier> getTier(Block block) {
		for (Block[] tiers : TIERED) {
			for (int i = 0; i < tiers.length; i++) {
				if (tiers[i] == block) {
					return Optional.of(StorageTier.values()[i]);
				}
			}
		}
		return Optional.empty();
	}

	/** The same kind of storage in another tier, when that one exists: enabled by the config. */
	public static Optional<Block> getInTier(Block block, StorageTier tier) {
		for (Block[] tiers : TIERED) {
			for (Block tierBlock : tiers) {
				if (tierBlock == block) {
					Block ret = tiers[tier.ordinal()];
					return getAllBlocks().contains(ret) ? Optional.of(ret) : Optional.empty();
				}
			}
		}
		return Optional.empty();
	}

	/** The barrel tiers the config enables; none with barrels off. */
	public static List<BarrelBlock> getBarrels() {
		return getEnabledTiers(Config.SERVER.barrelsEnabled, BARREL, COPPER_BARREL, IRON_BARREL, GOLD_BARREL, DIAMOND_BARREL, NETHERITE_BARREL);
	}

	/** Basic, copper, iron, gold, diamond and netherite, less what the config turns off. */
	@SafeVarargs
	private static <T extends Block> List<T> getEnabledTiers(boolean enabled, T... tiers) {
		List<T> ret = new ArrayList<>();
		if (!enabled) {
			return ret;
		}
		for (int i = 0; i < tiers.length; i++) {
			if (i == 1 && !Config.SERVER.copperTierEnabled || i == tiers.length - 1 && !Config.SERVER.netheriteTierEnabled) {
				continue;
			}
			ret.add(tiers[i]);
		}
		return ret;
	}

	/** The chest tiers the config enables; none with chests off. */
	public static List<ChestBlock> getChests() {
		return getEnabledTiers(Config.SERVER.chestsEnabled, CHEST, COPPER_CHEST, IRON_CHEST, GOLD_CHEST, DIAMOND_CHEST, NETHERITE_CHEST);
	}

	/** The shulker box tiers the config enables; none with shulker boxes off. */
	public static List<ShulkerBoxBlock> getShulkerBoxes() {
		return getEnabledTiers(Config.SERVER.shulkerBoxesEnabled, SHULKER_BOX, COPPER_SHULKER_BOX, IRON_SHULKER_BOX, GOLD_SHULKER_BOX, DIAMOND_SHULKER_BOX, NETHERITE_SHULKER_BOX);
	}

	public static List<Block> getAllBlocks() {
		List<Block> blocks = new ArrayList<>(getBarrels());
		blocks.addAll(getChests());
		blocks.addAll(getShulkerBoxes());
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
		GameRegistry.registerTileEntity(ShulkerBoxBlockEntity.class, DeconstructedStorage.getRL("shulker_box"));
	}
}
