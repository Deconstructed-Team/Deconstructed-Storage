package dev.feldsher.deconstructedstorage.upgrades.hopper;

import dev.feldsher.deconstructedcore.client.gui.StorageScreenBase;
import dev.feldsher.deconstructedcore.client.gui.UpgradeSettingsTab;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterControl;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.client.gui.SideIOControl;
import dev.feldsher.deconstructedstorage.client.gui.StorageButtonDefinitions;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public abstract class HopperUpgradeTab extends UpgradeSettingsTab<HopperUpgradeContainer> {
	protected ContentsFilterControl inputFilterLogicControl;
	protected ContentsFilterControl outputFilterLogicControl;

	protected HopperUpgradeTab(HopperUpgradeContainer container, Position position, StorageScreenBase<?> screen, String upgradeName) {
		super(container, position, screen, StorageTranslationHelper.INSTANCE.translUpgrade(upgradeName), tooltip(upgradeName));
	}

	private static ITextComponent tooltip(String upgradeName) {
		return new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translUpgradeKey(upgradeName) + TranslationHelper.TOOLTIP_SUFFIX);
	}

	@Override
	protected void moveSlotsToTab() {
		inputFilterLogicControl.moveSlotsToView();
		outputFilterLogicControl.moveSlotsToView();
	}

	public static class Basic extends HopperUpgradeTab {
		public Basic(HopperUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
			super(container, position, screen, "hopper");
			inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen, new Position(x + 3, y + 24), getContainer().getInputFilterLogicContainer(),
					Config.SERVER.hopperUpgrade.inputFilterSlotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
			outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Basic(screen,
					new Position(x + 3, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4), getContainer().getOutputFilterLogicContainer(),
					Config.SERVER.hopperUpgrade.outputFilterSlotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		}
	}

	public static class Advanced extends HopperUpgradeTab {
		public Advanced(HopperUpgradeContainer container, Position position, StorageScreenBase<?> screen) {
			super(container, position, screen, "advanced_hopper");
			inputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen, new Position(x + 3, y + 24), getContainer().getInputFilterLogicContainer(),
					Config.SERVER.advancedHopperUpgrade.inputFilterSlotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));

			SideIOControl sideIOControl = addHideableChild(new SideIOControl(getContainer().getSideIOContainer(),
					new Position(x + 3 + 9, inputFilterLogicControl.getY() + inputFilterLogicControl.getHeight() + 4)));

			outputFilterLogicControl = addHideableChild(new ContentsFilterControl.Advanced(screen,
					new Position(x + 3, sideIOControl.getY() + sideIOControl.getHeight() + 4), getContainer().getOutputFilterLogicContainer(),
					Config.SERVER.advancedHopperUpgrade.outputFilterSlotsInRow, StorageButtonDefinitions.STORAGE_CONTENTS_FILTER_TYPE));
		}
	}
}
