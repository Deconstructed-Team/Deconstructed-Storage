package dev.feldsher.deconstructedstorage.upgrades.compression;

import dev.feldsher.deconstructedcore.upgrades.IUpgradeCountLimitConfig;
import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeItem;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;

import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

/** In a storage, compacting and compression don't go together: they'd both rework the same items. */
public class StorageCompactingUpgradeItem extends CompactingUpgradeItem {
	private static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = Collections.singletonList(
			new UpgradeConflictDefinition(CompressionUpgradeItem.class::isInstance, 0, StorageTranslationHelper.INSTANCE.translError("add.compression_exists")));

	public StorageCompactingUpgradeItem(boolean shouldCompactThreeByThree, IntSupplier filterSlotCount, IUpgradeCountLimitConfig upgradeCountLimitConfig) {
		super(shouldCompactThreeByThree, filterSlotCount, upgradeCountLimitConfig);
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}
}
