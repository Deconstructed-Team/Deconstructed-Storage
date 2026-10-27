package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;

public class StorageTranslationHelper extends TranslationHelper {
	public static final StorageTranslationHelper INSTANCE = new StorageTranslationHelper();

	private StorageTranslationHelper() {
		super(DeconstructedStorage.MOD_ID);
	}
}
