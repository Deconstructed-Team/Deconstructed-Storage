package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.settings.StorageSettingsTabControlBase;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;
import dev.feldsher.deconstructedstorage.network.OpenStorageInventoryMessage;
import dev.feldsher.deconstructedstorage.network.PacketHandler;

public class StorageSettingsScreen extends SettingsScreen {
	public StorageSettingsScreen(StorageSettingsContainerMenu container) {
		super(container, container.getStorageWrapper().getDisplayName());
	}

	@Override
	protected StorageSettingsTabControlBase initializeTabControl() {
		return new StorageSettingsTabControl(this, new Position(guiLeft + xSize, guiTop + 4));
	}

	@Override
	protected void sendStorageInventoryScreenOpenMessage() {
		PacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage());
	}
}
