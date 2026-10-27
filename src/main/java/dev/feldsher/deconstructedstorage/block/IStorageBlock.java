package dev.feldsher.deconstructedstorage.block;

/** A block that's a storage: how big it is new. */
public interface IStorageBlock {
	int getNumberOfInventorySlots();

	int getNumberOfUpgradeSlots();

	default int getBaseStackSizeMultiplier() {
		return 1;
	}
}
