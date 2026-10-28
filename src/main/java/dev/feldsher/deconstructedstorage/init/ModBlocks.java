package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ControllerBlock;
import dev.feldsher.deconstructedstorage.block.ControllerBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageIOBlock;
import dev.feldsher.deconstructedstorage.block.StorageIOBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageLinkBlock;
import dev.feldsher.deconstructedstorage.block.StorageLinkBlockEntity;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlock;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlockEntity;
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

	public static final LimitedBarrelBlock LIMITED_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.limitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.limitedBarrel1.upgradeSlotCount, true), "limited_barrel_1");
	public static final LimitedBarrelBlock LIMITED_COPPER_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.copperLimitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.copperLimitedBarrel1.upgradeSlotCount, false), "limited_copper_barrel_1");
	public static final LimitedBarrelBlock LIMITED_IRON_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.ironLimitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.ironLimitedBarrel1.upgradeSlotCount, false), "limited_iron_barrel_1");
	public static final LimitedBarrelBlock LIMITED_GOLD_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.goldLimitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.goldLimitedBarrel1.upgradeSlotCount, false), "limited_gold_barrel_1");
	public static final LimitedBarrelBlock LIMITED_DIAMOND_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.diamondLimitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.diamondLimitedBarrel1.upgradeSlotCount, false), "limited_diamond_barrel_1");
	public static final LimitedBarrelBlock LIMITED_NETHERITE_BARREL_1 = block(new LimitedBarrelBlock(1, () -> Config.SERVER.netheriteLimitedBarrel1.baseSlotLimitMultiplier,
			() -> Config.SERVER.netheriteLimitedBarrel1.upgradeSlotCount, false), "limited_netherite_barrel_1");

	public static final LimitedBarrelBlock LIMITED_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.limitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.limitedBarrel2.upgradeSlotCount, true), "limited_barrel_2");
	public static final LimitedBarrelBlock LIMITED_COPPER_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.copperLimitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.copperLimitedBarrel2.upgradeSlotCount, false), "limited_copper_barrel_2");
	public static final LimitedBarrelBlock LIMITED_IRON_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.ironLimitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.ironLimitedBarrel2.upgradeSlotCount, false), "limited_iron_barrel_2");
	public static final LimitedBarrelBlock LIMITED_GOLD_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.goldLimitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.goldLimitedBarrel2.upgradeSlotCount, false), "limited_gold_barrel_2");
	public static final LimitedBarrelBlock LIMITED_DIAMOND_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.diamondLimitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.diamondLimitedBarrel2.upgradeSlotCount, false), "limited_diamond_barrel_2");
	public static final LimitedBarrelBlock LIMITED_NETHERITE_BARREL_2 = block(new LimitedBarrelBlock(2, () -> Config.SERVER.netheriteLimitedBarrel2.baseSlotLimitMultiplier,
			() -> Config.SERVER.netheriteLimitedBarrel2.upgradeSlotCount, false), "limited_netherite_barrel_2");

	public static final LimitedBarrelBlock LIMITED_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.limitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.limitedBarrel3.upgradeSlotCount, true), "limited_barrel_3");
	public static final LimitedBarrelBlock LIMITED_COPPER_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.copperLimitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.copperLimitedBarrel3.upgradeSlotCount, false), "limited_copper_barrel_3");
	public static final LimitedBarrelBlock LIMITED_IRON_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.ironLimitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.ironLimitedBarrel3.upgradeSlotCount, false), "limited_iron_barrel_3");
	public static final LimitedBarrelBlock LIMITED_GOLD_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.goldLimitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.goldLimitedBarrel3.upgradeSlotCount, false), "limited_gold_barrel_3");
	public static final LimitedBarrelBlock LIMITED_DIAMOND_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.diamondLimitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.diamondLimitedBarrel3.upgradeSlotCount, false), "limited_diamond_barrel_3");
	public static final LimitedBarrelBlock LIMITED_NETHERITE_BARREL_3 = block(new LimitedBarrelBlock(3, () -> Config.SERVER.netheriteLimitedBarrel3.baseSlotLimitMultiplier,
			() -> Config.SERVER.netheriteLimitedBarrel3.upgradeSlotCount, false), "limited_netherite_barrel_3");

	public static final LimitedBarrelBlock LIMITED_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.limitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.limitedBarrel4.upgradeSlotCount, true), "limited_barrel_4");
	public static final LimitedBarrelBlock LIMITED_COPPER_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.copperLimitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.copperLimitedBarrel4.upgradeSlotCount, false), "limited_copper_barrel_4");
	public static final LimitedBarrelBlock LIMITED_IRON_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.ironLimitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.ironLimitedBarrel4.upgradeSlotCount, false), "limited_iron_barrel_4");
	public static final LimitedBarrelBlock LIMITED_GOLD_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.goldLimitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.goldLimitedBarrel4.upgradeSlotCount, false), "limited_gold_barrel_4");
	public static final LimitedBarrelBlock LIMITED_DIAMOND_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.diamondLimitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.diamondLimitedBarrel4.upgradeSlotCount, false), "limited_diamond_barrel_4");
	public static final LimitedBarrelBlock LIMITED_NETHERITE_BARREL_4 = block(new LimitedBarrelBlock(4, () -> Config.SERVER.netheriteLimitedBarrel4.baseSlotLimitMultiplier,
			() -> Config.SERVER.netheriteLimitedBarrel4.upgradeSlotCount, false), "limited_netherite_barrel_4");

	public static final ControllerBlock CONTROLLER = block(new ControllerBlock(), "controller");
	public static final StorageLinkBlock STORAGE_LINK = block(new StorageLinkBlock(), "storage_link");
	public static final StorageIOBlock STORAGE_IO = block(new StorageIOBlock(StorageIOBlockEntity::new), "storage_io");
	public static final StorageIOBlock STORAGE_INPUT = block(new StorageIOBlock(StorageIOBlockEntity.Input::new), "storage_input");
	public static final StorageIOBlock STORAGE_OUTPUT = block(new StorageIOBlock(StorageIOBlockEntity.Output::new), "storage_output");

	static {
		// as 1.21's netherite storages, 1200: 1.12 keeps three times what it's given
		NETHERITE_BARREL.setResistance(400);
		NETHERITE_CHEST.setResistance(400);
		NETHERITE_SHULKER_BOX.setResistance(400);
		for (LimitedBarrelBlock barrel : new LimitedBarrelBlock[] {LIMITED_NETHERITE_BARREL_1, LIMITED_NETHERITE_BARREL_2, LIMITED_NETHERITE_BARREL_3, LIMITED_NETHERITE_BARREL_4}) {
			barrel.setResistance(400);
		}
	}

	private static final Block[][] TIERED = {
			{BARREL, COPPER_BARREL, IRON_BARREL, GOLD_BARREL, DIAMOND_BARREL, NETHERITE_BARREL},
			{CHEST, COPPER_CHEST, IRON_CHEST, GOLD_CHEST, DIAMOND_CHEST, NETHERITE_CHEST},
			{SHULKER_BOX, COPPER_SHULKER_BOX, IRON_SHULKER_BOX, GOLD_SHULKER_BOX, DIAMOND_SHULKER_BOX, NETHERITE_SHULKER_BOX},
			{LIMITED_BARREL_1, LIMITED_COPPER_BARREL_1, LIMITED_IRON_BARREL_1, LIMITED_GOLD_BARREL_1, LIMITED_DIAMOND_BARREL_1, LIMITED_NETHERITE_BARREL_1},
			{LIMITED_BARREL_2, LIMITED_COPPER_BARREL_2, LIMITED_IRON_BARREL_2, LIMITED_GOLD_BARREL_2, LIMITED_DIAMOND_BARREL_2, LIMITED_NETHERITE_BARREL_2},
			{LIMITED_BARREL_3, LIMITED_COPPER_BARREL_3, LIMITED_IRON_BARREL_3, LIMITED_GOLD_BARREL_3, LIMITED_DIAMOND_BARREL_3, LIMITED_NETHERITE_BARREL_3},
			{LIMITED_BARREL_4, LIMITED_COPPER_BARREL_4, LIMITED_IRON_BARREL_4, LIMITED_GOLD_BARREL_4, LIMITED_DIAMOND_BARREL_4, LIMITED_NETHERITE_BARREL_4}
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

	/** The limited barrels of one to four slots, of the tiers the config enables; none with barrels off. */
	public static List<LimitedBarrelBlock> getLimitedBarrels() {
		List<LimitedBarrelBlock> ret = new ArrayList<>();
		ret.addAll(getEnabledTiers(Config.SERVER.barrelsEnabled, LIMITED_BARREL_1, LIMITED_COPPER_BARREL_1, LIMITED_IRON_BARREL_1, LIMITED_GOLD_BARREL_1, LIMITED_DIAMOND_BARREL_1, LIMITED_NETHERITE_BARREL_1));
		ret.addAll(getEnabledTiers(Config.SERVER.barrelsEnabled, LIMITED_BARREL_2, LIMITED_COPPER_BARREL_2, LIMITED_IRON_BARREL_2, LIMITED_GOLD_BARREL_2, LIMITED_DIAMOND_BARREL_2, LIMITED_NETHERITE_BARREL_2));
		ret.addAll(getEnabledTiers(Config.SERVER.barrelsEnabled, LIMITED_BARREL_3, LIMITED_COPPER_BARREL_3, LIMITED_IRON_BARREL_3, LIMITED_GOLD_BARREL_3, LIMITED_DIAMOND_BARREL_3, LIMITED_NETHERITE_BARREL_3));
		ret.addAll(getEnabledTiers(Config.SERVER.barrelsEnabled, LIMITED_BARREL_4, LIMITED_COPPER_BARREL_4, LIMITED_IRON_BARREL_4, LIMITED_GOLD_BARREL_4, LIMITED_DIAMOND_BARREL_4, LIMITED_NETHERITE_BARREL_4));
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

	/** The controller and what joins its network besides storages. */
	public static List<Block> getNetworkBlocks() {
		List<Block> blocks = new ArrayList<>();
		blocks.add(CONTROLLER);
		blocks.add(STORAGE_LINK);
		blocks.add(STORAGE_IO);
		blocks.add(STORAGE_INPUT);
		blocks.add(STORAGE_OUTPUT);
		return blocks;
	}

	public static List<Block> getAllBlocks() {
		List<Block> blocks = new ArrayList<>(getBarrels());
		blocks.addAll(getLimitedBarrels());
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
		getNetworkBlocks().forEach(event.getRegistry()::register);
		GameRegistry.registerTileEntity(ControllerBlockEntity.class, DeconstructedStorage.getRL("controller"));
		GameRegistry.registerTileEntity(StorageLinkBlockEntity.class, DeconstructedStorage.getRL("storage_link"));
		GameRegistry.registerTileEntity(StorageIOBlockEntity.class, DeconstructedStorage.getRL("storage_io"));
		GameRegistry.registerTileEntity(StorageIOBlockEntity.Input.class, DeconstructedStorage.getRL("storage_input"));
		GameRegistry.registerTileEntity(StorageIOBlockEntity.Output.class, DeconstructedStorage.getRL("storage_output"));
		GameRegistry.registerTileEntity(BarrelBlockEntity.class, DeconstructedStorage.getRL("barrel"));
		GameRegistry.registerTileEntity(LimitedBarrelBlockEntity.class, DeconstructedStorage.getRL("limited_barrel"));
		GameRegistry.registerTileEntity(ChestBlockEntity.class, DeconstructedStorage.getRL("chest"));
		GameRegistry.registerTileEntity(ShulkerBoxBlockEntity.class, DeconstructedStorage.getRL("shulker_box"));
	}
}
