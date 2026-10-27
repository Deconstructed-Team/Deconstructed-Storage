package dev.feldsher.deconstructedstorage.client.gui;

import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.ITextComponent;
import java.util.List;
import java.util.ArrayList;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinitions;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterType;
import dev.feldsher.deconstructedstorage.upgrades.IOMode;

import java.util.HashMap;
import java.util.Map;

import static dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper.getButtonStateData;

public class StorageButtonDefinitions {
	public static final ButtonDefinition.Toggle<ContentsFilterType> STORAGE_CONTENTS_FILTER_TYPE;
	public static final ButtonDefinition.Toggle<Boolean> SHIFT_CLICK_TARGET = ButtonDefinitions.createToggleButtonDefinition(
			ButtonDefinitions.getBooleanStateData(
					getButtonStateData(new UV(32, 48), Dimension.SQUARE_16, new Position(1, 1), lines("shift_click_into_storage")),
					getButtonStateData(new UV(48, 48), Dimension.SQUARE_16, new Position(1, 1), lines("shift_click_into_inventory"))
			));

	public static final ButtonDefinition.Toggle<IOMode> IO_MODE;

	static {
		Map<IOMode, ToggleButton.StateData> ioModes = new HashMap<>();
		ioModes.put(IOMode.PUSH, getButtonStateData(new UV(208, 48), Dimension.SQUARE_16, new Position(1, 1), lines("io_mode_push")));
		ioModes.put(IOMode.PULL, getButtonStateData(new UV(192, 48), Dimension.SQUARE_16, new Position(1, 1), lines("io_mode_pull")));
		ioModes.put(IOMode.PUSH_PULL, getButtonStateData(new UV(176, 48), Dimension.SQUARE_16, new Position(1, 1), lines("io_mode_push_pull")));
		ioModes.put(IOMode.OFF, getButtonStateData(new UV(160, 48), Dimension.SQUARE_16, new Position(1, 1), lines("io_mode_off")));
		ioModes.put(IOMode.DISABLED, getButtonStateData(new UV(224, 48), Dimension.SQUARE_16, new Position(1, 1), lines("io_mode_disabled")));
		IO_MODE = ButtonDefinitions.createToggleButtonDefinition(ioModes);
	}

	static {
		Map<ContentsFilterType, ToggleButton.StateData> states = new HashMap<>();
		states.put(ContentsFilterType.ALLOW, getButtonStateData(new UV(0, 0), StorageTranslationHelper.INSTANCE.translUpgradeButton("allow"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(ContentsFilterType.BLOCK, getButtonStateData(new UV(16, 0), StorageTranslationHelper.INSTANCE.translUpgradeButton("block"), Dimension.SQUARE_16, new Position(1, 1)));
		states.put(ContentsFilterType.STORAGE, getButtonStateData(new UV(64, 16), StorageTranslationHelper.INSTANCE.translUpgradeButton("match_storage_contents"), Dimension.SQUARE_16, new Position(1, 1)));
		STORAGE_CONTENTS_FILTER_TYPE = ButtonDefinitions.createToggleButtonDefinition(states);
	}

	private static List<ITextComponent> lines(String buttonName) {
		List<ITextComponent> ret = new ArrayList<>();
		for (String line : StorageTranslationHelper.INSTANCE.getTranslatedLines(StorageTranslationHelper.INSTANCE.translUpgradeButton(buttonName))) {
			ret.add(new TextComponentString(line));
		}
		return ret;
	}

	private StorageButtonDefinitions() {}
}
