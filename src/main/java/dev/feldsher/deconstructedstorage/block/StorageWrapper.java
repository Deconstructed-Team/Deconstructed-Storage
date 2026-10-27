package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.common.gui.SortBy;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import dev.feldsher.deconstructedcore.storage.StorageContents;
import dev.feldsher.deconstructedcore.upgrades.UpgradeHandler;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import dev.feldsher.deconstructedcore.util.InventorySorter;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.settings.StorageSettingsHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A placed storage's contents and what it keeps about them. The contents themselves are in {@link StorageBlockContents},
 * by the UUID this holds; the inventory and upgrade handlers live with them, so that the block and anything holding a
 * handler, as a pipe, work on the one live inventory. The rest, as colors, sort order and slot counts, is saved with the
 * block, by its owner, the block entity, through {@link #save(NBTTagCompound)}.
 */
public abstract class StorageWrapper implements IStorageWrapper {
	private static final String UUID_TAG = "uuid";
	private static final String OPEN_TAB_ID_TAG = "openTabId";
	private static final String SORT_BY_TAG = "sortBy";
	private static final String COLUMNS_TAKEN_TAG = "columnsTaken";
	public static final String NUMBER_OF_INVENTORY_SLOTS_TAG = "numberOfInventorySlots";
	public static final String NUMBER_OF_UPGRADE_SLOTS_TAG = "numberOfUpgradeSlots";
	public static final String MAIN_COLOR_TAG = "mainColor";
	public static final String ACCENT_COLOR_TAG = "accentColor";

	private static final String INVENTORY_HANDLER = "inventory";
	private static final String UPGRADE_HANDLER = "upgrades";
	private static final String SETTINGS_HANDLER = "settings";

	private final Runnable saveHandler;
	private Runnable upgradeCachesInvalidatedHandler = () -> {
	};

	@Nullable
	private UUID contentsUuid = null;
	private int openTabId = -1;
	private SortBy sortBy = SortBy.NAME;
	private int columnsTaken = 0;
	private int numberOfInventorySlots = 0;
	private int numberOfUpgradeSlots = -1;
	private int mainColor = -1;
	private int accentColor = -1;

	protected StorageWrapper(Runnable saveHandler) {
		this.saveHandler = saveHandler;
	}

	public NBTTagCompound save(NBTTagCompound tag) {
		if (contentsUuid != null) {
			tag.setUniqueId(UUID_TAG, contentsUuid);
		}
		if (openTabId >= 0) {
			tag.setInteger(OPEN_TAB_ID_TAG, openTabId);
		}
		tag.setString(SORT_BY_TAG, sortBy.getSerializedName());
		if (columnsTaken > 0) {
			tag.setInteger(COLUMNS_TAKEN_TAG, columnsTaken);
		}
		if (numberOfInventorySlots > 0) {
			tag.setInteger(NUMBER_OF_INVENTORY_SLOTS_TAG, numberOfInventorySlots);
		}
		if (numberOfUpgradeSlots > -1) {
			tag.setInteger(NUMBER_OF_UPGRADE_SLOTS_TAG, numberOfUpgradeSlots);
		}
		if (mainColor != -1) {
			tag.setInteger(MAIN_COLOR_TAG, mainColor);
		}
		if (accentColor != -1) {
			tag.setInteger(ACCENT_COLOR_TAG, accentColor);
		}
		return tag;
	}

	public void load(NBTTagCompound tag) {
		contentsUuid = tag.hasUniqueId(UUID_TAG) ? tag.getUniqueId(UUID_TAG) : null;
		openTabId = tag.hasKey(OPEN_TAB_ID_TAG) ? tag.getInteger(OPEN_TAB_ID_TAG) : -1;
		sortBy = tag.hasKey(SORT_BY_TAG) ? SortBy.fromName(tag.getString(SORT_BY_TAG)) : SortBy.NAME;
		columnsTaken = tag.getInteger(COLUMNS_TAKEN_TAG);
		numberOfInventorySlots = tag.getInteger(NUMBER_OF_INVENTORY_SLOTS_TAG);
		numberOfUpgradeSlots = tag.hasKey(NUMBER_OF_UPGRADE_SLOTS_TAG) ? tag.getInteger(NUMBER_OF_UPGRADE_SLOTS_TAG) : -1;
		mainColor = tag.hasKey(MAIN_COLOR_TAG) ? tag.getInteger(MAIN_COLOR_TAG) : -1;
		accentColor = tag.hasKey(ACCENT_COLOR_TAG) ? tag.getInteger(ACCENT_COLOR_TAG) : -1;
	}

	protected void save() {
		saveHandler.run();
	}

	/** The storage's contents; made, with a new UUID, as they're first needed. */
	public StorageContents getContents() {
		return StorageBlockContents.getContents(getOrCreateContentsUuid());
	}

	private UUID getOrCreateContentsUuid() {
		if (contentsUuid == null) {
			contentsUuid = UUID.randomUUID();
			save();
		}
		return contentsUuid;
	}

	@Override
	public Optional<UUID> getContentsUuid() {
		return Optional.ofNullable(contentsUuid);
	}

	public void setContentsUuid(@Nullable UUID contentsUuid) {
		this.contentsUuid = contentsUuid;
		save();
	}

	/** Its contents are gone, the block broken: the UUID goes with them, so nothing finds them again. */
	public void removeContents() {
		if (contentsUuid != null) {
			StorageBlockContents.removeContents(contentsUuid);
			contentsUuid = null;
			save();
		}
	}

	@Override
	public InventoryHandler getInventoryHandler() {
		StorageContents contents = getContents();
		return contents.getShared(INVENTORY_HANDLER, () -> new InventoryHandler(getSavedSize(contents, InventoryHandler.INVENTORY_TAG, getExpectedInventorySlots()),
				this, contents.getNbt(), contents::onSlotChanged, StackUpgradeItem.getInventorySlotLimit(this), Config.SERVER.stackUpgrade.config) {
			@Override
			protected boolean isAllowed(ItemStack stack) {
				return isAllowedInStorage(stack);
			}
		});
	}

	private int getExpectedInventorySlots() {
		return getNumberOfInventorySlots() - getNumberOfSlotRows() * getColumnsTaken();
	}

	/** A handler is as big as it was saved; the counts here may say more, until {@link #changeSize} grows it. */
	private static int getSavedSize(StorageContents contents, String handlerTag, int defaultSize) {
		NBTTagCompound handlerNbt = contents.getNbt().getCompoundTag(handlerTag);
		return handlerNbt.hasKey("Size", Constants.NBT.TAG_INT) ? handlerNbt.getInteger("Size") : defaultSize;
	}

	@Override
	public UpgradeHandler getUpgradeHandler() {
		StorageContents contents = getContents();
		return contents.getShared(UPGRADE_HANDLER, () -> new UpgradeHandler(getSavedSize(contents, UpgradeHandler.UPGRADE_INVENTORY_TAG, getNumberOfUpgradeSlots()),
				this, contents.getNbt(), contents::markDirty, this::onUpgradesChanged) {
			@Override
			public boolean isItemValid(int slot, ItemStack stack) {
				return super.isItemValid(slot, stack) && (stack.isEmpty() || ModItems.isStorageUpgrade(stack.getItem()));
			}
		});
	}

	@Override
	public SettingsHandler getSettingsHandler() {
		StorageContents contents = getContents();
		int numberOfDisplayItems = getNumberOfDisplayItems();
		return contents.getShared(SETTINGS_HANDLER, () -> new StorageSettingsHandler(contents.getNbt(), contents::markDirty, this::getInventoryHandler) {
			@Override
			protected int getNumberOfDisplayItems() {
				return numberOfDisplayItems;
			}
		});
	}

	private void onUpgradesChanged() {
		refreshInventoryForUpgradeProcessing();
		getInventoryHandler().setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(this));
		onUpgradeRefresh();
		upgradeCachesInvalidatedHandler.run();
	}

	/** After the upgrades changed, as when the storage starts or stops ticking for them. */
	protected void onUpgradeRefresh() {
		//noop by default
	}

	@Override
	public void setUpgradeCachesInvalidatedHandler(Runnable handler) {
		upgradeCachesInvalidatedHandler = handler;
	}

	public int getNumberOfInventorySlots() {
		if (numberOfInventorySlots > 0) {
			return numberOfInventorySlots;
		}
		numberOfInventorySlots = getDefaultNumberOfInventorySlots();
		save();
		return numberOfInventorySlots;
	}

	private int getNumberOfUpgradeSlots() {
		if (numberOfUpgradeSlots > -1) {
			return numberOfUpgradeSlots;
		}
		numberOfUpgradeSlots = getDefaultNumberOfUpgradeSlots();
		save();
		return numberOfUpgradeSlots;
	}

	public abstract int getDefaultNumberOfInventorySlots();

	public abstract int getDefaultNumberOfUpgradeSlots();

	protected abstract boolean isAllowedInStorage(ItemStack stack);

	/** How many items the storage shows on itself: a barrel's front has room for four. */
	protected int getNumberOfDisplayItems() {
		return 1;
	}

	@Override
	public int getNumberOfSlotRows() {
		int itemInventorySlots = getNumberOfInventorySlots();
		return (int) Math.ceil(itemInventorySlots <= 81 ? (double) itemInventorySlots / 9 : (double) itemInventorySlots / 12);
	}

	/**
	 * A tier up, the shared handlers get the slots added, in place: what holds them, as another player's open container
	 * or a pipe, goes on with the one live inventory.
	 */
	public void changeSize(int additionalInventorySlots, int additionalUpgradeSlots) {
		numberOfInventorySlots = getNumberOfInventorySlots() + additionalInventorySlots;
		numberOfUpgradeSlots = getNumberOfUpgradeSlots() + additionalUpgradeSlots;
		if (additionalInventorySlots != 0) {
			getInventoryHandler().changeSlots(additionalInventorySlots);
		}
		if (additionalUpgradeSlots > 0) {
			getUpgradeHandler().increaseSize(additionalUpgradeSlots);
		}
		save();
	}

	@Override
	public Optional<Integer> getOpenTabId() {
		return openTabId >= 0 ? Optional.of(openTabId) : Optional.empty();
	}

	@Override
	public void setOpenTabId(int openTabId) {
		this.openTabId = openTabId;
		save();
	}

	@Override
	public void removeOpenTabId() {
		openTabId = -1;
		save();
	}

	@Override
	public int getMainColor() {
		return mainColor;
	}

	public boolean hasMainColor() {
		return mainColor != -1;
	}

	@Override
	public int getAccentColor() {
		return accentColor;
	}

	public boolean hasAccentColor() {
		return accentColor != -1;
	}

	@Override
	public void setColors(int mainColor, int accentColor) {
		this.mainColor = mainColor;
		this.accentColor = accentColor;
		save();
	}

	@Override
	public SortBy getSortBy() {
		return sortBy;
	}

	@Override
	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
		save();
	}

	/**
	 * Around the slots that settings leave out of sorting, the memorized slots, which keep what they remember, and the
	 * infinite ones, whose items can't be moved.
	 */
	@Override
	public void sort() {
		Set<Integer> slotIndexesExcludedFromSort = new HashSet<>(getSettingsHandler().getTypeCategory(NoSortSettingsCategory.class).getNoSortSlots());
		slotIndexesExcludedFromSort.addAll(getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).getSlotIndexes());
		InventoryHandler inventoryHandler = getInventoryHandler();
		slotIndexesExcludedFromSort.addAll(inventoryHandler.getInventoryPartitioner().getNoSortSlots());
		for (int slot = 0; slot < inventoryHandler.getSlots(); slot++) {
			if (inventoryHandler.isInfinite(slot)) {
				slotIndexesExcludedFromSort.add(slot);
			}
		}
		InventorySorter.sortHandler(inventoryHandler, InventorySorter.getComparator(getSortBy()), slotIndexesExcludedFromSort);
	}

	/**
	 * Changed, the inventory gets or gives up the columns' slots, in place, so that it stays the one live inventory.
	 * The slots given up were checked empty before the upgrade went in.
	 */
	@Override
	public void setColumnsTaken(int columnsTaken, boolean hasChanged) {
		int originalColumnsTaken = this.columnsTaken;
		this.columnsTaken = columnsTaken;
		if (hasChanged && originalColumnsTaken != columnsTaken && contentsUuid != null) {
			getInventoryHandler().changeSlots((originalColumnsTaken - columnsTaken) * getNumberOfSlotRows());
			refreshInventoryForUpgradeProcessing();
		}
		save();
	}

	@Override
	public int getColumnsTaken() {
		return columnsTaken;
	}

	/** The client's contents and their handlers were replaced as a whole when the server sent them: nothing to drop here. */
	@Override
	public void onContentsNbtUpdated() {
		//noop
	}

	@Override
	public void setContentsChangeHandler(Runnable contentsChangeHandler) {
		//noop: the block entity saves through the handler it made this with
	}

	@Override
	public void setPersistent(boolean persistent) {
		getInventoryHandler().setPersistent(persistent);
		getUpgradeHandler().setPersistent(persistent);
	}
}
