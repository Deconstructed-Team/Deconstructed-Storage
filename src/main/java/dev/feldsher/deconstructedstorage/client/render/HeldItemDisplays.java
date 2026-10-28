package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

/** What storages show more of while the player holds something that works on it, as 1.21's storage renderer asks. */
public final class HeldItemDisplays {
	private HeldItemDisplays() {}

	/** The storage tool, or an upgrade a storage takes: the upgrades show, empty slots too. */
	public static boolean holdsItemThatShowsUpgrades() {
		return holds(stack -> stack.getItem() instanceof StorageToolItem || ModItems.isStorageUpgrade(stack.getItem()));
	}

	/** The tool set to toggle the upgrades, over a storage that hides them: shown, they're crossed out. */
	public static boolean showsDisabledUpgrades(StorageBlockEntity storage) {
		return !storage.shouldShowUpgrades() && holds(stack -> stack.getItem() instanceof StorageToolItem
				&& StorageToolItem.getMode(stack) == StorageToolItem.Mode.UPGRADES_DISPLAY);
	}

	private static boolean holds(Predicate<ItemStack> matcher) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		return player != null && (matcher.test(player.getHeldItemMainhand()) || matcher.test(player.getHeldItemOffhand()));
	}
}
