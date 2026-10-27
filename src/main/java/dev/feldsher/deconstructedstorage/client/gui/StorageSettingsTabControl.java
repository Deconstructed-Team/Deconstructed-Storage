package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.Tab;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.settings.StorageSettingsTabControlBase;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsContainer;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsTab;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsContainer;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsContainer;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsTab;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsContainer;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsTab;

import java.util.HashMap;
import java.util.Map;

/** A storage's settings tabs: its main settings, no-sort, memory and item display. */
public class StorageSettingsTabControl extends StorageSettingsTabControlBase {
	private static final Map<String, ISettingsTabFactory<?, ?>> SETTINGS_TAB_FACTORIES = new HashMap<>();

	static {
		SETTINGS_TAB_FACTORIES.put(MainSettingsCategory.NAME, (ISettingsTabFactory<MainSettingsContainer, StorageMainSettingsTab>) StorageMainSettingsTab::new);
		SETTINGS_TAB_FACTORIES.put(NoSortSettingsCategory.NAME, (ISettingsTabFactory<NoSortSettingsContainer, NoSortSettingsTab>) NoSortSettingsTab::new);
		SETTINGS_TAB_FACTORIES.put(MemorySettingsCategory.NAME, (ISettingsTabFactory<MemorySettingsContainer, MemorySettingsTab>) MemorySettingsTab::new);
		SETTINGS_TAB_FACTORIES.put(ItemDisplaySettingsCategory.NAME, (ISettingsTabFactory<ItemDisplaySettingsContainer, ItemDisplaySettingsTab>) ItemDisplaySettingsTab::new);
	}

	public StorageSettingsTabControl(SettingsScreen screen, Position position) {
		super(screen, position);
	}

	@Override
	protected Tab instantiateReturnBackTab() {
		return new BackToStorageTab(new Position(x, getTopY()));
	}

	@Override
	protected Map<String, ISettingsTabFactory<?, ?>> getSettingsTabFactories() {
		return SETTINGS_TAB_FACTORIES;
	}
}
