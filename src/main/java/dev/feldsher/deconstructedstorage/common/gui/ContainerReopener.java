package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Opens a storage's container again once its upgrades changed, so that its settings tabs and their slots are built
 * anew: 1.12's containers can't change their slots while open. The reopening waits for the end of the tick, out of the
 * click that changed the upgrades, and keeps the stack the player holds on the cursor.
 */
@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public class ContainerReopener {
	private static final Map<EntityPlayerMP, Container> PENDING = new HashMap<>();

	private ContainerReopener() {}

	public static void scheduleReopen(EntityPlayerMP player, Container container) {
		PENDING.put(player, container);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) {
			return;
		}
		Map<EntityPlayerMP, Container> pending = new HashMap<>(PENDING);
		PENDING.clear();
		pending.forEach(ContainerReopener::reopen);
	}

	private static void reopen(EntityPlayerMP player, Container container) {
		if (player.openContainer != container || !(container instanceof StorageContainerMenu) || !container.canInteractWith(player)) {
			return;
		}
		StorageContainerMenu menu = (StorageContainerMenu) container;
		ItemStack cursorStack = player.inventory.getItemStack();
		player.inventory.setItemStack(ItemStack.EMPTY);
		menu.setReopening();
		GuiHandler.openStorage(player, menu.getStorageBlockEntity().getPos());
		player.inventory.setItemStack(cursorStack);
		player.updateHeldItem();
	}
}
