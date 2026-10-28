package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.settings.StorageSettingsTabControlBase;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;

/** A limited barrel's settings, its slots laid out as on its screen; it always shows them all, and sorts none. */
public class LimitedBarrelSettingsScreen extends StorageSettingsScreen {
	public LimitedBarrelSettingsScreen(StorageSettingsContainerMenu container) {
		super(container);
	}

	@Override
	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return LimitedBarrelScreen.STORAGE_SLOTS_HEIGHT;
	}

	@Override
	protected void updateStorageSlotsPositions() {
		LimitedBarrelScreen.updateSlotPositions(getMenu(), getMenu().getNumberOfStorageInventorySlots(), xSize);
	}

	@Override
	protected void drawSlotBg(int x, int y) {
		LimitedBarrelScreen.drawSlotBg(x, y, xSize, getMenu().getNumberOfStorageInventorySlots());
	}

	@Override
	protected StorageSettingsTabControlBase initializeTabControl() {
		return new StorageSettingsTabControl(this, new Position(guiLeft + xSize, guiTop + 4)) {
			@Override
			protected boolean isSettingsCategoryDisabled(String categoryName) {
				return categoryName.equals(ItemDisplaySettingsCategory.NAME) || categoryName.equals(NoSortSettingsCategory.NAME);
			}
		};
	}
}
