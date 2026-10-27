package dev.feldsher.deconstructedstorage.client.init;

import dev.feldsher.deconstructedcore.upgrades.xppump.XpPumpUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.pump.PumpUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.jukebox.JukeboxUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.cooking.AutoCookingUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.StonecutterUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.feeding.FeedingUpgradeTab;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.client.gui.StorageButtonDefinitions;
import dev.feldsher.deconstructedstorage.init.ModUpgradeContainers;
import dev.feldsher.deconstructedcore.client.gui.UpgradeGuiManager;
import dev.feldsher.deconstructedcore.upgrades.filter.FilterUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.filter.FilterUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.magnet.MagnetUpgradeTab;
import dev.feldsher.deconstructedstorage.upgrades.hopper.HopperUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.pickup.PickupUpgradeTab;
import dev.feldsher.deconstructedcore.upgrades.voiding.VoidUpgradeTab;

/** The settings tabs of the storage upgrades. */
public class ModUpgradeTabs {
	private ModUpgradeTabs() {}

	public static void register() {
		UpgradeGuiManager.registerTab(ModUpgradeContainers.PICKUP_BASIC_TYPE, (uc, p, s) ->
				new PickupUpgradeTab.Basic(uc, p, s, Config.SERVER.pickupUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.PICKUP_ADVANCED_TYPE, (uc, p, s) ->
				new PickupUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedPickupUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(FilterUpgradeContainer.BASIC_TYPE, (uc, p, s) ->
				new FilterUpgradeTab.Basic(uc, p, s, Config.SERVER.filterUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(FilterUpgradeContainer.ADVANCED_TYPE, (uc, p, s) ->
				new FilterUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedFilterUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.COMPACTING_BASIC_TYPE, (uc, p, s) ->
				new CompactingUpgradeTab.Basic(uc, p, s, Config.SERVER.compactingUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.COMPACTING_ADVANCED_TYPE, (uc, p, s) ->
				new CompactingUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedCompactingUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.FEEDING_BASIC_TYPE, (uc, p, s) ->
				new FeedingUpgradeTab.Basic(uc, p, s, Config.SERVER.feedingUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.FEEDING_ADVANCED_TYPE, (uc, p, s) ->
				new FeedingUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedFeedingUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.CRAFTING_TYPE, (uc, p, s) -> new CraftingUpgradeTab(uc, p, s, StorageButtonDefinitions.SHIFT_CLICK_TARGET));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.STONECUTTER_TYPE, (uc, p, s) -> new StonecutterUpgradeTab(uc, p, s, StorageButtonDefinitions.SHIFT_CLICK_TARGET));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.SMELTING_TYPE, (uc, p, s) -> new CookingUpgradeTab(uc, p, s, "smelting"));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.AUTO_SMELTING_TYPE, (uc, p, s) -> new AutoCookingUpgradeTab(uc, p, s, "auto_smelting",
				Config.SERVER.autoSmeltingUpgrade.inputFilterSlotsInRow, Config.SERVER.autoSmeltingUpgrade.fuelFilterSlotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.SMOKING_TYPE, (uc, p, s) -> new CookingUpgradeTab(uc, p, s, "smoking"));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.AUTO_SMOKING_TYPE, (uc, p, s) -> new AutoCookingUpgradeTab(uc, p, s, "auto_smoking",
				Config.SERVER.autoSmokingUpgrade.inputFilterSlotsInRow, Config.SERVER.autoSmokingUpgrade.fuelFilterSlotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.BLASTING_TYPE, (uc, p, s) -> new CookingUpgradeTab(uc, p, s, "blasting"));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.AUTO_BLASTING_TYPE, (uc, p, s) -> new AutoCookingUpgradeTab(uc, p, s, "auto_blasting",
				Config.SERVER.autoBlastingUpgrade.inputFilterSlotsInRow, Config.SERVER.autoBlastingUpgrade.fuelFilterSlotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.JUKEBOX_TYPE, JukeboxUpgradeTab.Basic::new);
		UpgradeGuiManager.registerTab(ModUpgradeContainers.ADVANCED_JUKEBOX_TYPE, (uc, p, s) -> new JukeboxUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedJukeboxUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.PUMP_TYPE, PumpUpgradeTab.Basic::new);
		UpgradeGuiManager.registerTab(ModUpgradeContainers.ADVANCED_PUMP_TYPE, PumpUpgradeTab.Advanced::new);
		UpgradeGuiManager.registerTab(ModUpgradeContainers.XP_PUMP_TYPE, (uc, p, s) -> new XpPumpUpgradeTab(uc, p, s, Config.SERVER.xpPumpUpgrade.mendingOn));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.HOPPER_BASIC_TYPE, HopperUpgradeTab.Basic::new);
		UpgradeGuiManager.registerTab(ModUpgradeContainers.HOPPER_ADVANCED_TYPE, HopperUpgradeTab.Advanced::new);
		UpgradeGuiManager.registerTab(ModUpgradeContainers.MAGNET_BASIC_TYPE, (uc, p, s) ->
				new MagnetUpgradeTab.Basic(uc, p, s, Config.SERVER.magnetUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.MAGNET_ADVANCED_TYPE, (uc, p, s) ->
				new MagnetUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedMagnetUpgrade.slotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.VOID_BASIC_TYPE, (uc, p, s) ->
				new VoidUpgradeTab.Basic(uc, p, s, Config.SERVER.voidUpgrade.slotsInRow));
		UpgradeGuiManager.registerTab(ModUpgradeContainers.VOID_ADVANCED_TYPE, (uc, p, s) ->
				new VoidUpgradeTab.Advanced(uc, p, s, Config.SERVER.advancedVoidUpgrade.slotsInRow));
	}
}
