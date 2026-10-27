package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedcore.upgrades.xppump.XpPumpUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.pump.PumpUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.pump.PumpUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.jukebox.JukeboxUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.cooking.AutoCookingUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.cooking.AutoCookingUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.cooking.CookingUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.StonecutterUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.stonecutter.StonecutterUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.crafting.CraftingUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.feeding.FeedingUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.feeding.FeedingUpgradeContainer;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerRegistry;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilteredUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.filter.FilterUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.magnet.MagnetUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.magnet.MagnetUpgradeWrapper;
import dev.feldsher.deconstructedstorage.upgrades.hopper.HopperUpgradeContainer;
import dev.feldsher.deconstructedstorage.upgrades.hopper.HopperUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.pickup.PickupUpgradeWrapper;
import dev.feldsher.deconstructedcore.upgrades.voiding.VoidUpgradeContainer;
import dev.feldsher.deconstructedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.minecraft.item.Item;

import java.util.Objects;

/** The containers behind the storage upgrades' settings tabs. */
public class ModUpgradeContainers {
	private ModUpgradeContainers() {}

	public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_BASIC_TYPE = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);
	public static final UpgradeContainerType<PickupUpgradeWrapper, ContentsFilteredUpgradeContainer<PickupUpgradeWrapper>> PICKUP_ADVANCED_TYPE = new UpgradeContainerType<>(ContentsFilteredUpgradeContainer::new);

	public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_BASIC_TYPE = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
	public static final UpgradeContainerType<CompactingUpgradeWrapper, CompactingUpgradeContainer> COMPACTING_ADVANCED_TYPE = new UpgradeContainerType<>(CompactingUpgradeContainer::new);
	public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_BASIC_TYPE = new UpgradeContainerType<>(FeedingUpgradeContainer::new);
	public static final UpgradeContainerType<FeedingUpgradeWrapper, FeedingUpgradeContainer> FEEDING_ADVANCED_TYPE = new UpgradeContainerType<>(FeedingUpgradeContainer::new);

	public static final UpgradeContainerType<CraftingUpgradeWrapper, CraftingUpgradeContainer> CRAFTING_TYPE = new UpgradeContainerType<>(CraftingUpgradeContainer::new);
	public static final UpgradeContainerType<StonecutterUpgradeWrapper, StonecutterUpgradeContainer> STONECUTTER_TYPE = new UpgradeContainerType<>(StonecutterUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper, CookingUpgradeContainer> SMELTING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper, AutoCookingUpgradeContainer> AUTO_SMELTING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper, CookingUpgradeContainer> SMOKING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper, AutoCookingUpgradeContainer> AUTO_SMOKING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<CookingUpgradeWrapper, CookingUpgradeContainer> BLASTING_TYPE = new UpgradeContainerType<>(CookingUpgradeContainer::new);
	public static final UpgradeContainerType<AutoCookingUpgradeWrapper, AutoCookingUpgradeContainer> AUTO_BLASTING_TYPE = new UpgradeContainerType<>(AutoCookingUpgradeContainer::new);
	public static final UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> JUKEBOX_TYPE = new UpgradeContainerType<>(JukeboxUpgradeContainer::new);
	public static final UpgradeContainerType<JukeboxUpgradeWrapper, JukeboxUpgradeContainer> ADVANCED_JUKEBOX_TYPE = new UpgradeContainerType<>(JukeboxUpgradeContainer::new);
	public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> PUMP_TYPE = new UpgradeContainerType<>(PumpUpgradeContainer::new);
	public static final UpgradeContainerType<PumpUpgradeWrapper, PumpUpgradeContainer> ADVANCED_PUMP_TYPE = new UpgradeContainerType<>(PumpUpgradeContainer::new);
	public static final UpgradeContainerType<XpPumpUpgradeWrapper, XpPumpUpgradeContainer> XP_PUMP_TYPE = new UpgradeContainerType<>(XpPumpUpgradeContainer::new);

	public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> HOPPER_BASIC_TYPE = new UpgradeContainerType<>(HopperUpgradeContainer::new);
	public static final UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> HOPPER_ADVANCED_TYPE = new UpgradeContainerType<>(HopperUpgradeContainer::new);
	public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_BASIC_TYPE = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
	public static final UpgradeContainerType<MagnetUpgradeWrapper, MagnetUpgradeContainer> MAGNET_ADVANCED_TYPE = new UpgradeContainerType<>(MagnetUpgradeContainer::new);
	public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_BASIC_TYPE = new UpgradeContainerType<>(VoidUpgradeContainer::new);
	public static final UpgradeContainerType<VoidUpgradeWrapper, VoidUpgradeContainer> VOID_ADVANCED_TYPE = new UpgradeContainerType<>(VoidUpgradeContainer::new);

	public static void register() {
		register(ModItems.PICKUP_UPGRADE, PICKUP_BASIC_TYPE);
		register(ModItems.ADVANCED_PICKUP_UPGRADE, PICKUP_ADVANCED_TYPE);
		register(ModItems.FILTER_UPGRADE, FilterUpgradeContainer.BASIC_TYPE);
		register(ModItems.ADVANCED_FILTER_UPGRADE, FilterUpgradeContainer.ADVANCED_TYPE);
		register(ModItems.COMPACTING_UPGRADE, COMPACTING_BASIC_TYPE);
		register(ModItems.ADVANCED_COMPACTING_UPGRADE, COMPACTING_ADVANCED_TYPE);
		register(ModItems.FEEDING_UPGRADE, FEEDING_BASIC_TYPE);
		register(ModItems.ADVANCED_FEEDING_UPGRADE, FEEDING_ADVANCED_TYPE);
		register(ModItems.CRAFTING_UPGRADE, CRAFTING_TYPE);
		register(ModItems.STONECUTTER_UPGRADE, STONECUTTER_TYPE);
		register(ModItems.SMELTING_UPGRADE, SMELTING_TYPE);
		register(ModItems.AUTO_SMELTING_UPGRADE, AUTO_SMELTING_TYPE);
		register(ModItems.SMOKING_UPGRADE, SMOKING_TYPE);
		register(ModItems.AUTO_SMOKING_UPGRADE, AUTO_SMOKING_TYPE);
		register(ModItems.BLASTING_UPGRADE, BLASTING_TYPE);
		register(ModItems.AUTO_BLASTING_UPGRADE, AUTO_BLASTING_TYPE);
		register(ModItems.JUKEBOX_UPGRADE, JUKEBOX_TYPE);
		register(ModItems.ADVANCED_JUKEBOX_UPGRADE, ADVANCED_JUKEBOX_TYPE);
		register(ModItems.PUMP_UPGRADE, PUMP_TYPE);
		register(ModItems.ADVANCED_PUMP_UPGRADE, ADVANCED_PUMP_TYPE);
		register(ModItems.XP_PUMP_UPGRADE, XP_PUMP_TYPE);
		register(ModItems.HOPPER_UPGRADE, HOPPER_BASIC_TYPE);
		register(ModItems.ADVANCED_HOPPER_UPGRADE, HOPPER_ADVANCED_TYPE);
		register(ModItems.MAGNET_UPGRADE, MAGNET_BASIC_TYPE);
		register(ModItems.ADVANCED_MAGNET_UPGRADE, MAGNET_ADVANCED_TYPE);
		register(ModItems.VOID_UPGRADE, VOID_BASIC_TYPE);
		register(ModItems.ADVANCED_VOID_UPGRADE, VOID_ADVANCED_TYPE);
	}

	private static void register(Item upgrade, UpgradeContainerType<?, ?> type) {
		UpgradeContainerRegistry.register(Objects.requireNonNull(upgrade.getRegistryName()), type);
	}
}
