package dev.feldsher.deconstructedstorage.settings;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.ISettingsCategory;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.main.MainSettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import net.minecraft.nbt.NBTTagCompound;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A storage's settings, under {@link #SETTINGS_TAG} in its contents. The player's own main settings are kept in the
 * persisted part of the player's data, which outlives death in 1.12, so nothing copies them over.
 */
public abstract class StorageSettingsHandler extends SettingsHandler {
	public static final String SETTINGS_TAG = "settings";
	public static final String PLAYER_SETTINGS_TAG = "deconstructedStorageSettings";

	protected StorageSettingsHandler(NBTTagCompound contentsNbt, Runnable markContentsDirty, Supplier<InventoryHandler> inventoryHandlerSupplier) {
		super(contentsNbt, markContentsDirty, inventoryHandlerSupplier);
	}

	@Override
	protected void addItemDisplayCategory(NBTTagCompound settingsNbt) {
		addSettingsCategory(settingsNbt, ItemDisplaySettingsCategory.NAME, markContentsDirty, (categoryNbt, saveNbt) ->
				new ItemDisplaySettingsCategory(inventoryHandlerSupplier, categoryNbt, saveNbt, getNumberOfDisplayItems(), () -> getTypeCategory(MemorySettingsCategory.class)));
	}

	/** How many items the storage shows: asked while the super constructor adds the categories, so not from a field. */
	protected abstract int getNumberOfDisplayItems();

	@Override
	protected NBTTagCompound getSettingsNbtFromContentsNbt(NBTTagCompound contentsNbt) {
		return contentsNbt.getCompoundTag(SETTINGS_TAG);
	}

	@Override
	protected void saveCategoryNbt(NBTTagCompound settingsNbt, String categoryName, NBTTagCompound tag) {
		settingsNbt.setTag(categoryName, tag);
		contentsNbt.setTag(SETTINGS_TAG, settingsNbt);
	}

	@Override
	public String getGlobalSettingsCategoryName() {
		return MainSettingsCategory.NAME;
	}

	@Override
	public ISettingsCategory<?> instantiateGlobalSettingsCategory(NBTTagCompound categoryNbt, Consumer<NBTTagCompound> saveNbt) {
		return new MainSettingsCategory<>(categoryNbt, saveNbt, PLAYER_SETTINGS_TAG);
	}
}
