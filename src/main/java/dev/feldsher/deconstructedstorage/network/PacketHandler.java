package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {
	public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(DeconstructedStorage.MOD_ID);

	private PacketHandler() {}

	public static void init() {
		int id = 0;
		INSTANCE.registerMessage(StorageContentsMessage.Handler.class, StorageContentsMessage.class, id++, Side.CLIENT);
		INSTANCE.registerMessage(OpenStorageInventoryMessage.Handler.class, OpenStorageInventoryMessage.class, id++, Side.SERVER);
		INSTANCE.registerMessage(ScrolledToolMessage.Handler.class, ScrolledToolMessage.class, id++, Side.SERVER);
	}
}
