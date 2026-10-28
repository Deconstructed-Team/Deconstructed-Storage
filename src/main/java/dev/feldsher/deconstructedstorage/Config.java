package dev.feldsher.deconstructedstorage;

import dev.feldsher.deconstructedcore.upgrades.cooking.IAutoCookingUpgradeConfig;
import dev.feldsher.deconstructedcore.upgrades.cooking.ICookingUpgradeConfig;
import dev.feldsher.deconstructedcore.upgrades.pump.IPumpUpgradeConfig;
import dev.feldsher.deconstructedcore.upgrades.xppump.IXpPumpUpgradeConfig;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeConfig;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Optional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@net.minecraftforge.common.config.Config(modid = DeconstructedStorage.MOD_ID)
@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public class Config {
	@net.minecraftforge.common.config.Config.Comment("Server Settings")
	@net.minecraftforge.common.config.Config.Name("server")
	public static final Server SERVER = new Server();

	private Config() {}

	public static class Server {
		@net.minecraftforge.common.config.Config.Comment("Whether the copper tier of storages exists. Its recipes take copper from the ore dictionary (ingotCopper); in a pack with none, they're left without recipes.")
		@net.minecraftforge.common.config.Config.RequiresMcRestart
		public boolean copperTierEnabled = false;
		@net.minecraftforge.common.config.Config.Comment("Whether the netherite tier of storages exists. Its recipes take netherite from the ore dictionary (ingotNetherite); in a pack with none, they're left without recipes.")
		@net.minecraftforge.common.config.Config.RequiresMcRestart
		public boolean netheriteTierEnabled = false;

		@net.minecraftforge.common.config.Config.Comment("Whether the barrels exist, of every tier. Off, they're not registered at all: a world that has them loses them.")
		@net.minecraftforge.common.config.Config.RequiresMcRestart
		public boolean barrelsEnabled = true;
		@net.minecraftforge.common.config.Config.Comment("Whether the chests exist, of every tier. Off, they're not registered at all: a world that has them loses them.")
		@net.minecraftforge.common.config.Config.RequiresMcRestart
		public boolean chestsEnabled = true;
		@net.minecraftforge.common.config.Config.Comment("Whether the shulker boxes exist, of every tier. Off, they're not registered at all: a world that has them loses them, with what they hold.")
		@net.minecraftforge.common.config.Config.RequiresMcRestart
		public boolean shulkerBoxesEnabled = true;

		@net.minecraftforge.common.config.Config.Comment("Determines whether chests / barrels are always dropped as packed with their contents when broken without the need to use packing tape")
		public boolean dropPacked = false;

		@net.minecraftforge.common.config.Config.Comment("Wood Barrel Settings")
		public final StorageConfig woodBarrel = new StorageConfig(27, 1);
		@net.minecraftforge.common.config.Config.Comment("Copper Barrel Settings")
		public final StorageConfig copperBarrel = new StorageConfig(45, 1);
		@net.minecraftforge.common.config.Config.Comment("Iron Barrel Settings")
		public final StorageConfig ironBarrel = new StorageConfig(54, 2);
		@net.minecraftforge.common.config.Config.Comment("Gold Barrel Settings")
		public final StorageConfig goldBarrel = new StorageConfig(81, 3);
		@net.minecraftforge.common.config.Config.Comment("Diamond Barrel Settings")
		public final StorageConfig diamondBarrel = new StorageConfig(108, 4);
		@net.minecraftforge.common.config.Config.Comment("Netherite Barrel Settings")
		public final StorageConfig netheriteBarrel = new StorageConfig(132, 5);

		@net.minecraftforge.common.config.Config.Comment("Wood Chest Settings")
		public final StorageConfig woodChest = new StorageConfig(27, 1);
		@net.minecraftforge.common.config.Config.Comment("Copper Chest Settings")
		public final StorageConfig copperChest = new StorageConfig(45, 1);
		@net.minecraftforge.common.config.Config.Comment("Iron Chest Settings")
		public final StorageConfig ironChest = new StorageConfig(54, 2);
		@net.minecraftforge.common.config.Config.Comment("Gold Chest Settings")
		public final StorageConfig goldChest = new StorageConfig(81, 3);
		@net.minecraftforge.common.config.Config.Comment("Diamond Chest Settings")
		public final StorageConfig diamondChest = new StorageConfig(108, 4);
		@net.minecraftforge.common.config.Config.Comment("Netherite Chest Settings")
		public final StorageConfig netheriteChest = new StorageConfig(132, 5);

		@net.minecraftforge.common.config.Config.Comment("Shulker Box Settings")
		public final StorageConfig shulkerBox = new StorageConfig(27, 1);
		@net.minecraftforge.common.config.Config.Comment("Copper Shulker Box Settings")
		public final StorageConfig copperShulkerBox = new StorageConfig(45, 1);
		@net.minecraftforge.common.config.Config.Comment("Iron Shulker Box Settings")
		public final StorageConfig ironShulkerBox = new StorageConfig(54, 2);
		@net.minecraftforge.common.config.Config.Comment("Gold Shulker Box Settings")
		public final StorageConfig goldShulkerBox = new StorageConfig(81, 3);
		@net.minecraftforge.common.config.Config.Comment("Diamond Shulker Box Settings")
		public final StorageConfig diamondShulkerBox = new StorageConfig(108, 4);
		@net.minecraftforge.common.config.Config.Comment("Netherite Shulker Box Settings")
		public final StorageConfig netheriteShulkerBox = new StorageConfig(132, 5);

		@net.minecraftforge.common.config.Config.Comment("Limited Barrel I Settings")
		public final LimitedBarrelConfig limitedBarrel1 = new LimitedBarrelConfig(32, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Copper Barrel I Settings")
		public final LimitedBarrelConfig copperLimitedBarrel1 = new LimitedBarrelConfig(53, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Iron Barrel I Settings")
		public final LimitedBarrelConfig ironLimitedBarrel1 = new LimitedBarrelConfig(64, 2);
		@net.minecraftforge.common.config.Config.Comment("Limited Gold Barrel I Settings")
		public final LimitedBarrelConfig goldLimitedBarrel1 = new LimitedBarrelConfig(96, 3);
		@net.minecraftforge.common.config.Config.Comment("Limited Diamond Barrel I Settings")
		public final LimitedBarrelConfig diamondLimitedBarrel1 = new LimitedBarrelConfig(128, 4);
		@net.minecraftforge.common.config.Config.Comment("Limited Netherite Barrel I Settings")
		public final LimitedBarrelConfig netheriteLimitedBarrel1 = new LimitedBarrelConfig(160, 5);

		@net.minecraftforge.common.config.Config.Comment("Limited Barrel II Settings")
		public final LimitedBarrelConfig limitedBarrel2 = new LimitedBarrelConfig(16, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Copper Barrel II Settings")
		public final LimitedBarrelConfig copperLimitedBarrel2 = new LimitedBarrelConfig(27, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Iron Barrel II Settings")
		public final LimitedBarrelConfig ironLimitedBarrel2 = new LimitedBarrelConfig(32, 2);
		@net.minecraftforge.common.config.Config.Comment("Limited Gold Barrel II Settings")
		public final LimitedBarrelConfig goldLimitedBarrel2 = new LimitedBarrelConfig(48, 3);
		@net.minecraftforge.common.config.Config.Comment("Limited Diamond Barrel II Settings")
		public final LimitedBarrelConfig diamondLimitedBarrel2 = new LimitedBarrelConfig(64, 4);
		@net.minecraftforge.common.config.Config.Comment("Limited Netherite Barrel II Settings")
		public final LimitedBarrelConfig netheriteLimitedBarrel2 = new LimitedBarrelConfig(80, 5);

		@net.minecraftforge.common.config.Config.Comment("Limited Barrel III Settings")
		public final LimitedBarrelConfig limitedBarrel3 = new LimitedBarrelConfig(10, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Copper Barrel III Settings")
		public final LimitedBarrelConfig copperLimitedBarrel3 = new LimitedBarrelConfig(17, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Iron Barrel III Settings")
		public final LimitedBarrelConfig ironLimitedBarrel3 = new LimitedBarrelConfig(20, 2);
		@net.minecraftforge.common.config.Config.Comment("Limited Gold Barrel III Settings")
		public final LimitedBarrelConfig goldLimitedBarrel3 = new LimitedBarrelConfig(30, 3);
		@net.minecraftforge.common.config.Config.Comment("Limited Diamond Barrel III Settings")
		public final LimitedBarrelConfig diamondLimitedBarrel3 = new LimitedBarrelConfig(40, 4);
		@net.minecraftforge.common.config.Config.Comment("Limited Netherite Barrel III Settings")
		public final LimitedBarrelConfig netheriteLimitedBarrel3 = new LimitedBarrelConfig(50, 5);

		@net.minecraftforge.common.config.Config.Comment("Limited Barrel IV Settings")
		public final LimitedBarrelConfig limitedBarrel4 = new LimitedBarrelConfig(8, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Copper Barrel IV Settings")
		public final LimitedBarrelConfig copperLimitedBarrel4 = new LimitedBarrelConfig(13, 1);
		@net.minecraftforge.common.config.Config.Comment("Limited Iron Barrel IV Settings")
		public final LimitedBarrelConfig ironLimitedBarrel4 = new LimitedBarrelConfig(16, 2);
		@net.minecraftforge.common.config.Config.Comment("Limited Gold Barrel IV Settings")
		public final LimitedBarrelConfig goldLimitedBarrel4 = new LimitedBarrelConfig(24, 3);
		@net.minecraftforge.common.config.Config.Comment("Limited Diamond Barrel IV Settings")
		public final LimitedBarrelConfig diamondLimitedBarrel4 = new LimitedBarrelConfig(32, 4);
		@net.minecraftforge.common.config.Config.Comment("Limited Netherite Barrel IV Settings")
		public final LimitedBarrelConfig netheriteLimitedBarrel4 = new LimitedBarrelConfig(40, 5);
		@net.minecraftforge.common.config.Config.Comment("List of items that are not allowed to be put in shulker boxes - e.g. \"minecraft:writable_book\". Shulker boxes themselves never are.")
		public String[] shulkerBoxDisallowedItems = new String[0];

		@net.minecraftforge.common.config.Config.Comment("Pickup Upgrade Settings")
		public final FilteredUpgradeConfig pickupUpgrade = new FilteredUpgradeConfig(9, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Pickup Upgrade Settings")
		public final FilteredUpgradeConfig advancedPickupUpgrade = new FilteredUpgradeConfig(16, 4);
		@net.minecraftforge.common.config.Config.Comment("Filter Upgrade Settings")
		public final FilteredUpgradeConfig filterUpgrade = new FilteredUpgradeConfig(9, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Filter Upgrade Settings")
		public final FilteredUpgradeConfig advancedFilterUpgrade = new FilteredUpgradeConfig(16, 4);
		@net.minecraftforge.common.config.Config.Comment("Compacting Upgrade Settings")
		public final FilteredUpgradeConfig compactingUpgrade = new FilteredUpgradeConfig(9, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Compacting Upgrade Settings")
		public final FilteredUpgradeConfig advancedCompactingUpgrade = new FilteredUpgradeConfig(16, 4);
		@net.minecraftforge.common.config.Config.Comment("Feeding Upgrade Settings")
		public final FilteredUpgradeConfig feedingUpgrade = new FilteredUpgradeConfig(9, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Feeding Upgrade Settings")
		public final FilteredUpgradeConfig advancedFeedingUpgrade = new FilteredUpgradeConfig(16, 4);
		@net.minecraftforge.common.config.Config.Comment("Magnet Upgrade Settings")
		public final MagnetUpgradeConfig magnetUpgrade = new MagnetUpgradeConfig(9, 3, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Magnet Upgrade Settings")
		public final MagnetUpgradeConfig advancedMagnetUpgrade = new MagnetUpgradeConfig(16, 4, 5);
		@net.minecraftforge.common.config.Config.Comment("Void Upgrade Settings")
		public final VoidUpgradeConfig voidUpgrade = new VoidUpgradeConfig(9, 3);
		@net.minecraftforge.common.config.Config.Comment("Advanced Void Upgrade Settings")
		public final VoidUpgradeConfig advancedVoidUpgrade = new VoidUpgradeConfig(16, 4);

		@net.minecraftforge.common.config.Config.Comment("Hopper Upgrade Settings")
		public final HopperUpgradeConfig hopperUpgrade = new HopperUpgradeConfig(2, 2, 2, 2, 8, 1);
		@net.minecraftforge.common.config.Config.Comment("Advanced Hopper Upgrade Settings")
		public final HopperUpgradeConfig advancedHopperUpgrade = new HopperUpgradeConfig(4, 4, 4, 4, 2, 4);

		@net.minecraftforge.common.config.Config.Comment("Smelting Upgrade Settings")
		public final CookingUpgradeConfig smeltingUpgrade = new CookingUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Smoking Upgrade Settings")
		public final CookingUpgradeConfig smokingUpgrade = new CookingUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Blasting Upgrade Settings")
		public final CookingUpgradeConfig blastingUpgrade = new CookingUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Auto-Smelting Upgrade Settings")
		public final AutoCookingUpgradeConfig autoSmeltingUpgrade = new AutoCookingUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Auto-Smoking Upgrade Settings")
		public final AutoCookingUpgradeConfig autoSmokingUpgrade = new AutoCookingUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Auto-Blasting Upgrade Settings")
		public final AutoCookingUpgradeConfig autoBlastingUpgrade = new AutoCookingUpgradeConfig();

		@net.minecraftforge.common.config.Config.Comment("Advanced Jukebox Upgrade Settings")
		public final JukeboxUpgradeConfig advancedJukeboxUpgrade = new JukeboxUpgradeConfig(12);
		@net.minecraftforge.common.config.Config.Comment("Compression Upgrade Settings")
		public final CompressionUpgradeConfig compressionUpgrade = new CompressionUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Pump Upgrade Settings")
		public final PumpUpgradeConfig pumpUpgrade = new PumpUpgradeConfig();
		@net.minecraftforge.common.config.Config.Comment("Xp Pump Upgrade Settings")
		public final XpPumpUpgradeConfig xpPumpUpgrade = new XpPumpUpgradeConfig();

		@net.minecraftforge.common.config.Config.Comment("Stack Upgrade Settings")
		public final StackUpgradeSettings stackUpgrade = new StackUpgradeSettings();

		@net.minecraftforge.common.config.Config.Comment("Limit of maximum number of upgrades of type per storage in format of \"StorageType|UpgradeRegistryName[or UpgradeGroup]|MaxNumber\"")
		public String[] maxUpgradesPerStorage = {
				"chest|stack_upgrades|2", "chest|furnace_upgrades|1", "chest|jukebox_upgrades|1",
				"barrel|stack_upgrades|2", "barrel|furnace_upgrades|1", "barrel|jukebox_upgrades|1",
				"shulker_box|stack_upgrades|2", "shulker_box|furnace_upgrades|1", "shulker_box|jukebox_upgrades|1",
				"limited_barrel|furnace_upgrades|1", "limited_barrel|jukebox_upgrades|1"};

		@net.minecraftforge.common.config.Config.Ignore
		public final IUpgradeCountLimitConfig maxUpgradesPerStorageConfig = new IUpgradeCountLimitConfig() {
			@Override
			public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
				return upgradeRegistryName == null ? Integer.MAX_VALUE
						: getMaxUpgrades().getOrDefault(storageType, Collections.emptyMap()).getOrDefault(upgradeRegistryName.getPath(), Integer.MAX_VALUE);
			}

			@Override
			public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
				return getMaxUpgrades().getOrDefault(storageType, Collections.emptyMap()).getOrDefault(upgradeGroup.name(), Integer.MAX_VALUE);
			}
		};

		@net.minecraftforge.common.config.Config.Ignore
		@Nullable
		private Map<String, Map<String, Integer>> maxUpgrades = null;
		@net.minecraftforge.common.config.Config.Ignore
		@Nullable
		private Set<ResourceLocation> shulkerBoxDisallowedItemsSet = null;

		public boolean isItemDisallowedInShulkerBox(Item item) {
			if (shulkerBoxDisallowedItemsSet == null) {
				shulkerBoxDisallowedItemsSet = new HashSet<>();
				for (String itemName : shulkerBoxDisallowedItems) {
					shulkerBoxDisallowedItemsSet.add(new ResourceLocation(itemName));
				}
			}
			return shulkerBoxDisallowedItemsSet.contains(item.getRegistryName());
		}

		private Map<String, Map<String, Integer>> getMaxUpgrades() {
			if (maxUpgrades == null) {
				maxUpgrades = new HashMap<>();
				for (String mapping : maxUpgradesPerStorage) {
					String[] parts = mapping.split("\\|");
					if (parts.length < 3 || !parts[2].matches("\\d+")) {
						continue;
					}
					maxUpgrades.computeIfAbsent(parts[0], k -> new HashMap<>()).put(parts[1], Integer.parseInt(parts[2]));
				}
			}
			return maxUpgrades;
		}

		private void clearCache() {
			maxUpgrades = null;
			shulkerBoxDisallowedItemsSet = null;
			stackUpgrade.config.clearNonStackableItems();
			compressionUpgrade.clearCache();
		}
	}

	public static class StorageConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of inventory slots in the storage")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 180)
		public int inventorySlotCount;
		@net.minecraftforge.common.config.Config.Comment("Number of upgrade slots in the storage")
		@net.minecraftforge.common.config.Config.RangeInt(min = 0, max = 10)
		public int upgradeSlotCount;

		StorageConfig(int inventorySlotCountDefault, int upgradeSlotCountDefault) {
			inventorySlotCount = inventorySlotCountDefault;
			upgradeSlotCount = upgradeSlotCountDefault;
		}
	}

	public static class LimitedBarrelConfig {
		@net.minecraftforge.common.config.Config.Comment("Multiplier that's used to calculate base slot limit")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 8192)
		public int baseSlotLimitMultiplier;
		@net.minecraftforge.common.config.Config.Comment("Number of upgrade slots in the storage")
		@net.minecraftforge.common.config.Config.RangeInt(min = 0, max = 10)
		public int upgradeSlotCount;

		LimitedBarrelConfig(int baseSlotLimitMultiplierDefault, int upgradeSlotCountDefault) {
			baseSlotLimitMultiplier = baseSlotLimitMultiplierDefault;
			upgradeSlotCount = upgradeSlotCountDefault;
		}
	}

	public static class FilteredUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of the upgrade's filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int filterSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int slotsInRow;

		FilteredUpgradeConfig(int defaultFilterSlots, int defaultSlotsInRow) {
			filterSlots = defaultFilterSlots;
			slotsInRow = defaultSlotsInRow;
		}
	}

	// Forge's config reads the fields a class declares, not inherited ones: each config class has all of its own

	public static class MagnetUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of the upgrade's filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int filterSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int slotsInRow;
		@net.minecraftforge.common.config.Config.Comment("Range around storage in blocks at which magnet will pickup items")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int magnetRange;

		MagnetUpgradeConfig(int defaultFilterSlots, int defaultSlotsInRow, int defaultMagnetRange) {
			filterSlots = defaultFilterSlots;
			slotsInRow = defaultSlotsInRow;
			magnetRange = defaultMagnetRange;
		}
	}

	public static class CompressionUpgradeConfig {
		private static final Pattern DECOMPRESSIBLE_MATCHER = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_/.-]+)=(\\d+)x([a-z0-9_.-]+:[a-z0-9_/.-]+)");

		@net.minecraftforge.common.config.Config.Comment("Defines how many slots at a maximum compression upgrade is able to use")
		@net.minecraftforge.common.config.Config.RangeInt(min = 3, max = 9)
		public int maxNumberOfSlots = 5;
		@net.minecraftforge.common.config.Config.Comment("List of items that can be decompressed by compression upgrade and their results. "
				+ "Item registry names are expected here in format of \"mod:itemBeingDecompressed=Nxmod:itemDecompressResult\"")
		public String[] additionalDecompressibleItems = {"minecraft:glowstone=4xminecraft:glowstone_dust", "minecraft:quartz_block=4xminecraft:quartz"};

		@net.minecraftforge.common.config.Config.Ignore
		@Nullable
		private Map<Item, RecipeHelper.UncompactingResult> additionalDecompressibleItemsMap = null;

		public Optional<RecipeHelper.UncompactingResult> getDecompressionResult(Item item) {
			if (additionalDecompressibleItemsMap == null) {
				additionalDecompressibleItemsMap = new HashMap<>();
				for (String decompressibleItem : additionalDecompressibleItems) {
					Matcher matcher = DECOMPRESSIBLE_MATCHER.matcher(decompressibleItem);
					if (matcher.find()) {
						Item fromItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(matcher.group(1)));
						int count = Integer.parseInt(matcher.group(2));
						Item toItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(matcher.group(3)));
						if (fromItem != null && toItem != null && fromItem != Items.AIR && toItem != Items.AIR && (count == 4 || count == 9)) {
							additionalDecompressibleItemsMap.put(fromItem, new RecipeHelper.UncompactingResult(new ItemStack(toItem),
									count == 4 ? RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE : RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE));
						}
					}
				}
			}
			return Optional.ofNullable(additionalDecompressibleItemsMap.get(item));
		}

		void clearCache() {
			additionalDecompressibleItemsMap = null;
		}
	}

	public static class HopperUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of input filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 8)
		public int inputFilterSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of input filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 4)
		public int inputFilterSlotsInRow;
		@net.minecraftforge.common.config.Config.Comment("Number of output filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 8)
		public int outputFilterSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of output filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 4)
		public int outputFilterSlotsInRow;
		@net.minecraftforge.common.config.Config.Comment("Number of ticks between each transfer")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 100)
		public int transferSpeedTicks;
		@net.minecraftforge.common.config.Config.Comment("Maximum stack size that can be transferred in one transfer")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 64)
		public int maxTransferStackSize;

		HopperUpgradeConfig(int defaultInputFilterSlots, int defaultInputFilterSlotsInRow, int defaultOutputFilterSlots, int defaultOutputFilterSlotsInRow,
				int defaultTransferSpeedTicks, int defaultMaxTransferStackSize) {
			inputFilterSlots = defaultInputFilterSlots;
			inputFilterSlotsInRow = defaultInputFilterSlotsInRow;
			outputFilterSlots = defaultOutputFilterSlots;
			outputFilterSlotsInRow = defaultOutputFilterSlotsInRow;
			transferSpeedTicks = defaultTransferSpeedTicks;
			maxTransferStackSize = defaultMaxTransferStackSize;
		}
	}

	public static class VoidUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of the upgrade's filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int filterSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int slotsInRow;
		@net.minecraftforge.common.config.Config.Comment("Determines whether void upgrade allows voiding anything or it only has overflow option")
		public boolean voidAnythingEnabled = true;

		VoidUpgradeConfig(int defaultFilterSlots, int defaultSlotsInRow) {
			filterSlots = defaultFilterSlots;
			slotsInRow = defaultSlotsInRow;
		}
	}

	public static class JukeboxUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of slots for discs in jukebox upgrade")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 16)
		public int numberOfSlots;
		@net.minecraftforge.common.config.Config.Comment("Number of lots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int slotsInRow = 4;

		JukeboxUpgradeConfig(int defaultNumberOfSlots) {
			numberOfSlots = defaultNumberOfSlots;
		}
	}

	public static class PumpUpgradeConfig implements IPumpUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Number of fluid filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int filterSlots = 4;
		@net.minecraftforge.common.config.Config.Comment("How much mB can be transfered in / out per operation. This is a base transfer rate that gets multiplied by number of rows in storage and stack multiplier.")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 1000)
		public int maxInputOutput = 20;
		@net.minecraftforge.common.config.Config.Comment("Ratio that gets applied (multiplies) to inventory stack multiplier before this is applied to max input/output value. Value lower than 1 makes stack multiplier affect the capacity less, higher makes it affect the capacity more. 0 turns off stack multiplier affecting input/output")
		@net.minecraftforge.common.config.Config.RangeDouble(min = 0, max = 5)
		public double stackMultiplierRatio = 1;

		@Override
		public int getFilterSlots() {
			return filterSlots;
		}

		@Override
		public int getMaxInputOutput() {
			return maxInputOutput;
		}

		@Override
		public double getStackMultiplierRatio() {
			return stackMultiplierRatio;
		}
	}

	public static class XpPumpUpgradeConfig implements IXpPumpUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Whether xp pump can mend items with mending. Set false here to turn off the feature altogether.")
		public boolean mendingOn = true;
		@net.minecraftforge.common.config.Config.Comment("How many experience points at a maximum would be used to mend an item per operation (every 5 ticks and 1 xp point usually translates to 2 damage repaired).")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int maxXpPointsPerMending = 5;

		@Override
		public boolean isMendingOn() {
			return mendingOn;
		}

		@Override
		public int getMaxXpPointsPerMending() {
			return maxXpPointsPerMending;
		}
	}

	public static class CookingUpgradeConfig implements ICookingUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Smelting speed multiplier (1.0 equals speed at which vanilla furnace smelts items)")
		@net.minecraftforge.common.config.Config.RangeDouble(min = 0.25, max = 4.0)
		public double smeltingSpeedMultiplier = 1.0;
		@net.minecraftforge.common.config.Config.Comment("Fuel efficiency multiplier (1.0 equals speed at which it's used in vanilla furnace)")
		@net.minecraftforge.common.config.Config.RangeDouble(min = 0.25, max = 4.0)
		public double fuelEfficiencyMultiplier = 1.0;

		@Override
		public double getCookingSpeedMultiplier() {
			return smeltingSpeedMultiplier;
		}

		@Override
		public double getFuelEfficiencyMultiplier() {
			return fuelEfficiencyMultiplier;
		}
	}

	public static class AutoCookingUpgradeConfig implements IAutoCookingUpgradeConfig {
		@net.minecraftforge.common.config.Config.Comment("Smelting speed multiplier (1.0 equals speed at which vanilla furnace smelts items)")
		@net.minecraftforge.common.config.Config.RangeDouble(min = 0.25, max = 4.0)
		public double smeltingSpeedMultiplier = 1.0;
		@net.minecraftforge.common.config.Config.Comment("Fuel efficiency multiplier (1.0 equals speed at which it's used in vanilla furnace)")
		@net.minecraftforge.common.config.Config.RangeDouble(min = 0.25, max = 4.0)
		public double fuelEfficiencyMultiplier = 1.0;
		@net.minecraftforge.common.config.Config.Comment("Number of input filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int inputFilterSlots = 8;
		@net.minecraftforge.common.config.Config.Comment("Number of input filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int inputFilterSlotsInRow = 4;
		@net.minecraftforge.common.config.Config.Comment("Number of fuel filter slots")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 20)
		public int fuelFilterSlots = 4;
		@net.minecraftforge.common.config.Config.Comment("Number of fuel filter slots displayed in a row")
		@net.minecraftforge.common.config.Config.RangeInt(min = 1, max = 6)
		public int fuelFilterSlotsInRow = 4;

		@Override
		public double getCookingSpeedMultiplier() {
			return smeltingSpeedMultiplier;
		}

		@Override
		public double getFuelEfficiencyMultiplier() {
			return fuelEfficiencyMultiplier;
		}

		@Override
		public int getInputFilterSlots() {
			return inputFilterSlots;
		}

		@Override
		public int getInputFilterSlotsInRow() {
			return inputFilterSlotsInRow;
		}

		@Override
		public int getFuelFilterSlots() {
			return fuelFilterSlots;
		}

		@Override
		public int getFuelFilterSlotsInRow() {
			return fuelFilterSlotsInRow;
		}
	}

	public static class StackUpgradeSettings {
		@net.minecraftforge.common.config.Config.Comment("List of items that are not supposed to stack in storage even when stack upgrade is inserted. Item registry names are expected here.")
		public String[] nonStackableItems = StackUpgradeConfig.getDefaultNonStackableList();

		@net.minecraftforge.common.config.Config.Ignore
		public final StackUpgradeConfig config = new StackUpgradeConfig(() -> nonStackableItems);
	}

	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(DeconstructedStorage.MOD_ID)) {
			ConfigManager.sync(DeconstructedStorage.MOD_ID, net.minecraftforge.common.config.Config.Type.INSTANCE);
			SERVER.clearCache();
		}
	}
}
