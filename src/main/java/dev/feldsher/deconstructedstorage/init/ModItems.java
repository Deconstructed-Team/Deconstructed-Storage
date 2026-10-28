package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.cooking.AutoCookingUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingType;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.feeding.FeedingUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.filter.FilterUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.infinity.InfinityUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.jukebox.JukeboxUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.magnet.MagnetUpgradeItem;
import dev.feldsher.deconstructedstorage.upgrades.compression.CompressionUpgradeItem;
import dev.feldsher.deconstructedstorage.upgrades.compression.StorageCompactingUpgradeItem;
import dev.feldsher.deconstructedstorage.upgrades.hopper.HopperUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.pickup.PickupUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.pump.PumpUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.StonecutterUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.voiding.VoidUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.xppump.XpPumpUpgradeItem;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlock;
import dev.feldsher.deconstructedstorage.block.StorageTier;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.ChestBlockItem;
import dev.feldsher.deconstructedstorage.item.PackingTapeItem;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItem;
import dev.feldsher.deconstructedstorage.item.StorageTierUpgradeItem;
import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public class ModItems {
	private ModItems() {}

	public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(DeconstructedStorage.MOD_ID) {
		@Override
		public ItemStack createIcon() {
			List<net.minecraft.block.Block> blocks = ModBlocks.getAllBlocks();
			// the gold barrel, or with barrels off whatever's left
			return WoodStorageBlockItem.setWoodType(new ItemStack(blocks.contains(ModBlocks.GOLD_BARREL) || blocks.isEmpty() ? ModBlocks.GOLD_BARREL : blocks.get(0)), WoodType.SPRUCE);
		}
	};

	public static final StorageTierUpgradeItem BASIC_TIER_UPGRADE = item(new StorageTierUpgradeItem(null, StorageTier.BASIC), "basic_tier_upgrade");
	public static final StorageTierUpgradeItem BASIC_TO_COPPER_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.BASIC, StorageTier.COPPER), "basic_to_copper_tier_upgrade");
	public static final StorageTierUpgradeItem BASIC_TO_IRON_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.BASIC, StorageTier.IRON), "basic_to_iron_tier_upgrade");
	public static final StorageTierUpgradeItem BASIC_TO_GOLD_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.BASIC, StorageTier.GOLD), "basic_to_gold_tier_upgrade");
	public static final StorageTierUpgradeItem BASIC_TO_DIAMOND_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.BASIC, StorageTier.DIAMOND), "basic_to_diamond_tier_upgrade");
	public static final StorageTierUpgradeItem BASIC_TO_NETHERITE_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.BASIC, StorageTier.NETHERITE), "basic_to_netherite_tier_upgrade");
	public static final StorageTierUpgradeItem COPPER_TO_IRON_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.COPPER, StorageTier.IRON), "copper_to_iron_tier_upgrade");
	public static final StorageTierUpgradeItem COPPER_TO_GOLD_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.COPPER, StorageTier.GOLD), "copper_to_gold_tier_upgrade");
	public static final StorageTierUpgradeItem COPPER_TO_DIAMOND_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.COPPER, StorageTier.DIAMOND), "copper_to_diamond_tier_upgrade");
	public static final StorageTierUpgradeItem COPPER_TO_NETHERITE_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.COPPER, StorageTier.NETHERITE), "copper_to_netherite_tier_upgrade");
	public static final StorageTierUpgradeItem IRON_TO_GOLD_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.IRON, StorageTier.GOLD), "iron_to_gold_tier_upgrade");
	public static final StorageTierUpgradeItem IRON_TO_DIAMOND_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.IRON, StorageTier.DIAMOND), "iron_to_diamond_tier_upgrade");
	public static final StorageTierUpgradeItem IRON_TO_NETHERITE_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.IRON, StorageTier.NETHERITE), "iron_to_netherite_tier_upgrade");
	public static final StorageTierUpgradeItem GOLD_TO_DIAMOND_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.GOLD, StorageTier.DIAMOND), "gold_to_diamond_tier_upgrade");
	public static final StorageTierUpgradeItem GOLD_TO_NETHERITE_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.GOLD, StorageTier.NETHERITE), "gold_to_netherite_tier_upgrade");
	public static final StorageTierUpgradeItem DIAMOND_TO_NETHERITE_TIER_UPGRADE = item(new StorageTierUpgradeItem(StorageTier.DIAMOND, StorageTier.NETHERITE), "diamond_to_netherite_tier_upgrade");

	public static final PackingTapeItem PACKING_TAPE = item(new PackingTapeItem(), "packing_tape");
	public static final StorageToolItem STORAGE_TOOL = item(new StorageToolItem(), "storage_tool");

	private static final List<StorageTierUpgradeItem> TIER_UPGRADES = Arrays.asList(BASIC_TIER_UPGRADE, BASIC_TO_COPPER_TIER_UPGRADE, BASIC_TO_IRON_TIER_UPGRADE,
			BASIC_TO_GOLD_TIER_UPGRADE, BASIC_TO_DIAMOND_TIER_UPGRADE, BASIC_TO_NETHERITE_TIER_UPGRADE, COPPER_TO_IRON_TIER_UPGRADE, COPPER_TO_GOLD_TIER_UPGRADE,
			COPPER_TO_DIAMOND_TIER_UPGRADE, COPPER_TO_NETHERITE_TIER_UPGRADE, IRON_TO_GOLD_TIER_UPGRADE, IRON_TO_DIAMOND_TIER_UPGRADE, IRON_TO_NETHERITE_TIER_UPGRADE,
			GOLD_TO_DIAMOND_TIER_UPGRADE, GOLD_TO_NETHERITE_TIER_UPGRADE, DIAMOND_TO_NETHERITE_TIER_UPGRADE);

	/** The tier upgrades between tiers the config enables. */
	public static List<StorageTierUpgradeItem> getTierUpgrades() {
		List<StorageTierUpgradeItem> ret = new ArrayList<>();
		for (StorageTierUpgradeItem upgrade : TIER_UPGRADES) {
			if (upgrade.isEnabled()) {
				ret.add(upgrade);
			}
		}
		return ret;
	}

	public static final Item UPGRADE_BASE = item(new Item(), "upgrade_base");

	public static final PickupUpgradeItem PICKUP_UPGRADE = item(new PickupUpgradeItem(() -> Config.SERVER.pickupUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "pickup_upgrade");
	public static final PickupUpgradeItem ADVANCED_PICKUP_UPGRADE = item(new PickupUpgradeItem(() -> Config.SERVER.advancedPickupUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "advanced_pickup_upgrade");
	public static final FilterUpgradeItem FILTER_UPGRADE = item(new FilterUpgradeItem(() -> Config.SERVER.filterUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "filter_upgrade");
	public static final FilterUpgradeItem ADVANCED_FILTER_UPGRADE = item(new FilterUpgradeItem(() -> Config.SERVER.advancedFilterUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "advanced_filter_upgrade");
	public static final MagnetUpgradeItem MAGNET_UPGRADE = item(new MagnetUpgradeItem(() -> Config.SERVER.magnetUpgrade.magnetRange,
			() -> Config.SERVER.magnetUpgrade.filterSlots, Config.SERVER.maxUpgradesPerStorageConfig), "magnet_upgrade");
	public static final MagnetUpgradeItem ADVANCED_MAGNET_UPGRADE = item(new MagnetUpgradeItem(() -> Config.SERVER.advancedMagnetUpgrade.magnetRange,
			() -> Config.SERVER.advancedMagnetUpgrade.filterSlots, Config.SERVER.maxUpgradesPerStorageConfig), "advanced_magnet_upgrade");
	public static final FeedingUpgradeItem FEEDING_UPGRADE = item(new FeedingUpgradeItem(() -> Config.SERVER.feedingUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "feeding_upgrade");
	public static final FeedingUpgradeItem ADVANCED_FEEDING_UPGRADE = item(new FeedingUpgradeItem(() -> Config.SERVER.advancedFeedingUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "advanced_feeding_upgrade");
	public static final CompactingUpgradeItem COMPACTING_UPGRADE = item(new StorageCompactingUpgradeItem(false, () -> Config.SERVER.compactingUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "compacting_upgrade");
	public static final CompactingUpgradeItem ADVANCED_COMPACTING_UPGRADE = item(new StorageCompactingUpgradeItem(true, () -> Config.SERVER.advancedCompactingUpgrade.filterSlots,
			Config.SERVER.maxUpgradesPerStorageConfig), "advanced_compacting_upgrade");
	public static final VoidUpgradeItem VOID_UPGRADE = item(new VoidUpgradeItem(() -> Config.SERVER.voidUpgrade.filterSlots,
			() -> Config.SERVER.voidUpgrade.voidAnythingEnabled, Config.SERVER.maxUpgradesPerStorageConfig), "void_upgrade");
	public static final VoidUpgradeItem ADVANCED_VOID_UPGRADE = item(new VoidUpgradeItem(() -> Config.SERVER.advancedVoidUpgrade.filterSlots,
			() -> Config.SERVER.advancedVoidUpgrade.voidAnythingEnabled, Config.SERVER.maxUpgradesPerStorageConfig), "advanced_void_upgrade");
	public static final CookingUpgradeItem SMELTING_UPGRADE = item(new CookingUpgradeItem(CookingType.SMELTING, Config.SERVER.smeltingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "smelting_upgrade");
	public static final AutoCookingUpgradeItem AUTO_SMELTING_UPGRADE = item(new AutoCookingUpgradeItem(CookingType.SMELTING, Config.SERVER.autoSmeltingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "auto_smelting_upgrade");
	public static final CookingUpgradeItem SMOKING_UPGRADE = item(new CookingUpgradeItem(CookingType.SMOKING, Config.SERVER.smokingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "smoking_upgrade");
	public static final AutoCookingUpgradeItem AUTO_SMOKING_UPGRADE = item(new AutoCookingUpgradeItem(CookingType.SMOKING, Config.SERVER.autoSmokingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "auto_smoking_upgrade");
	public static final CookingUpgradeItem BLASTING_UPGRADE = item(new CookingUpgradeItem(CookingType.BLASTING, Config.SERVER.blastingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "blasting_upgrade");
	public static final AutoCookingUpgradeItem AUTO_BLASTING_UPGRADE = item(new AutoCookingUpgradeItem(CookingType.BLASTING, Config.SERVER.autoBlastingUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "auto_blasting_upgrade");
	public static final CraftingUpgradeItem CRAFTING_UPGRADE = item(new CraftingUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig), "crafting_upgrade");
	public static final StonecutterUpgradeItem STONECUTTER_UPGRADE = item(new StonecutterUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig), "stonecutter_upgrade");
	public static final JukeboxUpgradeItem JUKEBOX_UPGRADE = item(new JukeboxUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig, () -> 1, () -> 1), "jukebox_upgrade");
	public static final JukeboxUpgradeItem ADVANCED_JUKEBOX_UPGRADE = item(new JukeboxUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig,
			() -> Config.SERVER.advancedJukeboxUpgrade.numberOfSlots, () -> Config.SERVER.advancedJukeboxUpgrade.slotsInRow), "advanced_jukebox_upgrade");
	// as 1.21's storage pumps: the basic one works with the world only, a placed storage has no hands in reach
	public static final PumpUpgradeItem PUMP_UPGRADE = item(new PumpUpgradeItem(false, false, Config.SERVER.pumpUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "pump_upgrade");
	public static final PumpUpgradeItem ADVANCED_PUMP_UPGRADE = item(new PumpUpgradeItem(true, true, Config.SERVER.pumpUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "advanced_pump_upgrade");
	public static final XpPumpUpgradeItem XP_PUMP_UPGRADE = item(new XpPumpUpgradeItem(Config.SERVER.xpPumpUpgrade, Config.SERVER.maxUpgradesPerStorageConfig), "xp_pump_upgrade");
	public static final InfinityUpgradeItem INFINITY_UPGRADE = item(new InfinityUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig, true), "infinity_upgrade");
	public static final InfinityUpgradeItem SURVIVAL_INFINITY_UPGRADE = item(new InfinityUpgradeItem(Config.SERVER.maxUpgradesPerStorageConfig, false), "survival_infinity_upgrade");

	public static final CompressionUpgradeItem COMPRESSION_UPGRADE = item(new CompressionUpgradeItem(), "compression_upgrade");
	public static final HopperUpgradeItem HOPPER_UPGRADE = item(new HopperUpgradeItem(() -> Config.SERVER.hopperUpgrade.inputFilterSlots,
			() -> Config.SERVER.hopperUpgrade.outputFilterSlots, () -> Config.SERVER.hopperUpgrade.transferSpeedTicks,
			() -> Config.SERVER.hopperUpgrade.maxTransferStackSize, Config.SERVER.maxUpgradesPerStorageConfig), "hopper_upgrade");
	public static final HopperUpgradeItem ADVANCED_HOPPER_UPGRADE = item(new HopperUpgradeItem(() -> Config.SERVER.advancedHopperUpgrade.inputFilterSlots,
			() -> Config.SERVER.advancedHopperUpgrade.outputFilterSlots, () -> Config.SERVER.advancedHopperUpgrade.transferSpeedTicks,
			() -> Config.SERVER.advancedHopperUpgrade.maxTransferStackSize, Config.SERVER.maxUpgradesPerStorageConfig), "advanced_hopper_upgrade");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_1 = item(new StackUpgradeItem(2, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_1");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_1_PLUS = item(new StackUpgradeItem(3, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_1_plus");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_2 = item(new StackUpgradeItem(4, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_2");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_3 = item(new StackUpgradeItem(8, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_3");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_4 = item(new StackUpgradeItem(16, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_4");
	public static final StackUpgradeItem STACK_UPGRADE_TIER_5 = item(new StackUpgradeItem(32, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_tier_5");
	public static final StackUpgradeItem STACK_DOWNGRADE_TIER_1 = item(new StackUpgradeItem(0.125, Config.SERVER.maxUpgradesPerStorageConfig), "stack_downgrade_tier_1");
	public static final StackUpgradeItem STACK_DOWNGRADE_TIER_2 = item(new StackUpgradeItem(0.0625, Config.SERVER.maxUpgradesPerStorageConfig), "stack_downgrade_tier_2");
	public static final StackUpgradeItem STACK_DOWNGRADE_TIER_3 = item(new StackUpgradeItem(0.03125, Config.SERVER.maxUpgradesPerStorageConfig), "stack_downgrade_tier_3");
	public static final StackUpgradeItem STACK_UPGRADE_OMEGA_TIER = item(new StackUpgradeItem(Integer.MAX_VALUE, Config.SERVER.maxUpgradesPerStorageConfig), "stack_upgrade_omega_tier");

	/** What 1.21 tags as storage upgrades: the only items a storage's upgrade slots take. */
	public static final List<Item> STORAGE_UPGRADES = Collections.unmodifiableList(Arrays.asList(PICKUP_UPGRADE, ADVANCED_PICKUP_UPGRADE,
			FILTER_UPGRADE, ADVANCED_FILTER_UPGRADE, MAGNET_UPGRADE, ADVANCED_MAGNET_UPGRADE, FEEDING_UPGRADE, ADVANCED_FEEDING_UPGRADE,
			COMPACTING_UPGRADE, ADVANCED_COMPACTING_UPGRADE, VOID_UPGRADE, ADVANCED_VOID_UPGRADE, SMELTING_UPGRADE, AUTO_SMELTING_UPGRADE,
			SMOKING_UPGRADE, AUTO_SMOKING_UPGRADE, BLASTING_UPGRADE, AUTO_BLASTING_UPGRADE, CRAFTING_UPGRADE, STONECUTTER_UPGRADE,
			JUKEBOX_UPGRADE, ADVANCED_JUKEBOX_UPGRADE, PUMP_UPGRADE, ADVANCED_PUMP_UPGRADE, XP_PUMP_UPGRADE, INFINITY_UPGRADE, SURVIVAL_INFINITY_UPGRADE,
			STACK_UPGRADE_TIER_1, STACK_UPGRADE_TIER_1_PLUS, STACK_UPGRADE_TIER_2, STACK_UPGRADE_TIER_3, STACK_UPGRADE_TIER_4, STACK_UPGRADE_TIER_5,
			STACK_DOWNGRADE_TIER_1, STACK_DOWNGRADE_TIER_2, STACK_DOWNGRADE_TIER_3, STACK_UPGRADE_OMEGA_TIER, COMPRESSION_UPGRADE, HOPPER_UPGRADE, ADVANCED_HOPPER_UPGRADE));

	public static boolean isStorageUpgrade(Item item) {
		return STORAGE_UPGRADES.contains(item);
	}

	public static List<Item> getAllItems() {
		List<Item> items = new ArrayList<>();
		for (BarrelBlock barrel : ModBlocks.getBarrels()) {
			items.add(itemBlock(new BarrelBlockItem(barrel)));
		}
		for (ChestBlock chest : ModBlocks.getChests()) {
			items.add(itemBlock(new ChestBlockItem(chest)));
		}
		for (ShulkerBoxBlock shulkerBox : ModBlocks.getShulkerBoxes()) {
			items.add(itemBlock(new ShulkerBoxItem(shulkerBox)));
		}
		items.addAll(getTierUpgrades());
		items.add(UPGRADE_BASE);
		items.add(PACKING_TAPE);
		items.add(STORAGE_TOOL);
		items.addAll(STORAGE_UPGRADES);
		return items;
	}

	private static <T extends Item> T item(T item, String name) {
		item.setRegistryName(DeconstructedStorage.getRL(name));
		item.setTranslationKey(DeconstructedStorage.MOD_ID + "." + name);
		item.setCreativeTab(CREATIVE_TAB);
		return item;
	}

	private static ItemBlock itemBlock(ItemBlock item) {
		item.setRegistryName(Objects.requireNonNull(item.getBlock().getRegistryName()));
		return item;
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		getAllItems().forEach(event.getRegistry()::register);
	}
}
