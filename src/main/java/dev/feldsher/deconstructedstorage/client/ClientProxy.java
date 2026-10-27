package dev.feldsher.deconstructedstorage.client;

import dev.feldsher.deconstructedstorage.CommonProxy;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.client.render.ChestRenderer;
import dev.feldsher.deconstructedstorage.client.gui.StorageScreen;
import dev.feldsher.deconstructedstorage.client.gui.StorageSettingsScreen;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;
import net.minecraftforge.fml.client.registry.ClientRegistry;

@SuppressWarnings("unused") // instantiated by @SidedProxy
public class ClientProxy extends CommonProxy {
	@Override
	public void preInit() {
		ClientRegistry.bindTileEntitySpecialRenderer(ChestBlockEntity.class, new ChestRenderer());
	}

	@Override
	public Object createStorageScreen(StorageContainerMenu container) {
		return new StorageScreen(container);
	}

	@Override
	public Object createStorageSettingsScreen(StorageSettingsContainerMenu container) {
		return new StorageSettingsScreen(container);
	}
}
