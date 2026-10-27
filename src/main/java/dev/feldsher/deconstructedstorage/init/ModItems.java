package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.WoodType;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public class ModItems {
	private ModItems() {}

	public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(DeconstructedStorage.MOD_ID) {
		@Override
		public ItemStack createIcon() {
			return WoodStorageBlockItem.setWoodType(new ItemStack(ModBlocks.GOLD_BARREL), WoodType.SPRUCE);
		}
	};

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
			items.add(itemBlock(new WoodStorageBlockItem(chest)));
		}
		items.addAll(STORAGE_UPGRADES);
		return items;
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
