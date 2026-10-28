package dev.feldsher.deconstructedstorage.init;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItemEntity;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public final class ModEntities {
	private ModEntities() {}

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		// Tracked as vanilla tracks item entities
		event.getRegistry().register(EntityEntryBuilder.create()
				.entity(ShulkerBoxItemEntity.class)
				.id(DeconstructedStorage.getRL("shulker_box_item"), 0)
				.name(DeconstructedStorage.MOD_ID + ".shulker_box_item")
				.tracker(64, 20, true)
				.build());
	}
}
