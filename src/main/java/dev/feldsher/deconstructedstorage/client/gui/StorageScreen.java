package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;

public class StorageScreen extends StorageScreenBase<StorageContainerMenu> {
	public StorageScreen(StorageContainerMenu menu) {
		super(menu, menu.getStorageWrapper().getDisplayName());
	}

	@Override
	protected String getStorageSettingsTabTooltip() {
		return StorageTranslationHelper.INSTANCE.translGui("settings.tooltip");
	}
}
