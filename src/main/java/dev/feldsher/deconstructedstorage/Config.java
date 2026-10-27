package dev.feldsher.deconstructedstorage;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.UpgradeGroup;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

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

		@net.minecraftforge.common.config.Config.Comment("Stack Upgrade Settings")
		public final StackUpgradeSettings stackUpgrade = new StackUpgradeSettings();

		@net.minecraftforge.common.config.Config.Comment("Maximum number of upgrades of type per storage in format of \"UpgradeRegistryName[or UpgradeGroup]|MaxNumber\"")
		public String[] maxUpgradesPerStorage = {"stack_upgrades|3", "jukebox_upgrades|1", "furnace_upgrades|1"};

		@net.minecraftforge.common.config.Config.Ignore
		public final IUpgradeCountLimitConfig maxUpgradesPerStorageConfig = new IUpgradeCountLimitConfig() {
			@Override
			public int getMaxUpgradesPerStorage(String storageType, @Nullable ResourceLocation upgradeRegistryName) {
				return upgradeRegistryName == null ? Integer.MAX_VALUE : getMaxUpgrades().getOrDefault(upgradeRegistryName.getPath(), Integer.MAX_VALUE);
			}

			@Override
			public int getMaxUpgradesInGroupPerStorage(String storageType, UpgradeGroup upgradeGroup) {
				return getMaxUpgrades().getOrDefault(upgradeGroup.name(), Integer.MAX_VALUE);
			}
		};

		@net.minecraftforge.common.config.Config.Ignore
		@Nullable
		private Map<String, Integer> maxUpgrades = null;

		private Map<String, Integer> getMaxUpgrades() {
			if (maxUpgrades == null) {
				maxUpgrades = new HashMap<>();
				for (String mapping : maxUpgradesPerStorage) {
					String[] upgradeMax = mapping.split("\\|");
					if (upgradeMax.length < 2 || !upgradeMax[1].matches("\\d+")) {
						continue;
					}
					maxUpgrades.put(upgradeMax[0], Integer.parseInt(upgradeMax[1]));
				}
			}
			return maxUpgrades;
		}

		private void clearCache() {
			maxUpgrades = null;
			stackUpgrade.config.clearNonStackableItems();
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
