package dev.feldsher.deconstructedstorage.client;

import dev.feldsher.deconstructedstorage.CommonProxy;
import dev.feldsher.deconstructedstorage.block.BarrelBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ControllerBlockEntity;
import dev.feldsher.deconstructedstorage.client.render.ControllerRenderer;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlockEntity;
import dev.feldsher.deconstructedstorage.client.gui.LimitedBarrelScreen;
import dev.feldsher.deconstructedstorage.client.gui.LimitedBarrelSettingsScreen;
import dev.feldsher.deconstructedstorage.client.render.LimitedBarrelRenderer;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlockEntity;
import dev.feldsher.deconstructedstorage.client.render.BarrelRenderer;
import dev.feldsher.deconstructedstorage.client.render.ChestRenderer;
import dev.feldsher.deconstructedstorage.client.render.ShulkerBoxRenderer;
import dev.feldsher.deconstructedstorage.client.gui.StorageScreen;
import dev.feldsher.deconstructedstorage.client.init.ModUpgradeTabs;
import dev.feldsher.deconstructedstorage.client.gui.StorageSettingsScreen;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;
import net.minecraftforge.fml.client.registry.ClientRegistry;

@SuppressWarnings("unused") // instantiated by @SidedProxy
public class ClientProxy extends CommonProxy {
	@Override
	public void preInit() {
		ClientRegistry.bindTileEntitySpecialRenderer(BarrelBlockEntity.class, new BarrelRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(LimitedBarrelBlockEntity.class, new LimitedBarrelRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(ControllerBlockEntity.class, new ControllerRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(ChestBlockEntity.class, new ChestRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(ShulkerBoxBlockEntity.class, new ShulkerBoxRenderer());
	}

	@Override
	public void init() {
		ModUpgradeTabs.register();
	}

	@Override
	public Object createStorageScreen(StorageContainerMenu container) {
		return container.getStorageBlockEntity() instanceof LimitedBarrelBlockEntity ? new LimitedBarrelScreen(container) : new StorageScreen(container);
	}

	@Override
	public Object createStorageSettingsScreen(StorageSettingsContainerMenu container) {
		return container.getStorageBlockEntity() instanceof LimitedBarrelBlockEntity ? new LimitedBarrelSettingsScreen(container) : new StorageSettingsScreen(container);
	}
}
