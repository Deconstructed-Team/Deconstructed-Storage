package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.SettingsScreen;
import dev.feldsher.deconstructedcore.client.gui.controls.ItemButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsContainer;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsTab;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Arrays;
import java.util.List;

/** The storage's main settings, its tab showing the storage itself. */
public class StorageMainSettingsTab extends MainSettingsTab<MainSettingsContainer> {
	private static final List<ITextComponent> CONTEXT_TOOLTIP = Arrays.asList(
			new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip")),
			gray(new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage.tooltip_detail")))
	);

	public StorageMainSettingsTab(MainSettingsContainer container, Position position, SettingsScreen screen) {
		super(container, position, screen, CONTEXT_TOOLTIP, new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translSettingsButton("context_storage")),
				StorageTranslationHelper.INSTANCE.translSettings("storage"), StorageTranslationHelper.INSTANCE.translSettingsTooltip("storage"),
				onTabIconClicked -> new ItemButton(new Position(position.x() + 1, position.y() + 4), onTabIconClicked,
						container.getSettingsContainer().getStorageWrapper().getWrappedStorageStack()));
	}
}
