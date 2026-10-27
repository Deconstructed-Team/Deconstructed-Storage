package dev.feldsher.deconstructedstorage.init;

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

	/** What 1.21 tags as storage upgrades: the only items a storage's upgrade slots take. */
	public static final List<Item> STORAGE_UPGRADES = Collections.emptyList();

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
