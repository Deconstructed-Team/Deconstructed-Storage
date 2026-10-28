package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.upgrades.voiding.VoidUpgradeWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A placed limited barrel. Its counts and fill levels go to clients with its render info; it keeps whether it shows
 * them, the colors of the counts, and its flat top, which its block's state has no room for.
 */
public class LimitedBarrelBlockEntity extends BarrelBlockEntity implements ICountDisplay, IFillLevelDisplay {
	public static final String STORAGE_TYPE = "limited_barrel";
	private static final String SHOW_COUNTS_TAG = "showCounts";
	private static final String SHOW_FILL_LEVELS_TAG = "showFillLevels";
	private static final String SLOT_COLORS_TAG = "slotColors";
	private static final String FLAT_TOP_TAG = "flatTop";
	/** A void upgrade in one voids the overflow of anything by default: it's for keeping the barrel's slots from filling up. */
	public static final Consumer<VoidUpgradeWrapper> VOID_UPGRADE_VOIDING_OVERFLOW_OF_EVERYTHING_BY_DEFAULT = voidUpgrade -> {
		voidUpgrade.getFilterLogic().setAllowByDefault(false);
		voidUpgrade.setShouldVoidOverflowDefaultOrLoadFromNbt(true);
	};
	// 1.21's text colors of the dyes, which 1.12's dye colors don't have
	private static final Map<EnumDyeColor, Integer> TEXT_COLORS = new EnumMap<>(EnumDyeColor.class);

	static {
		int[] textColors = {0xFFFFFF, 0xFF681F, 0xFF00FF, 0x9AC0CD, 0xFFFF00, 0xBFFF00, 0xFF69B4, 0x808080, 0xD3D3D3, 0x00FFFF, 0xA020F0, 0x0000FF, 0x8B4513,
				0x00FF00, 0xFF0000, 0x000000};
		for (EnumDyeColor color : EnumDyeColor.values()) {
			TEXT_COLORS.put(color, textColors[color.getMetadata()]);
		}
	}

	private long lastDepositTime = -100;
	private Map<Integer, EnumDyeColor> slotColors = new HashMap<>();
	private boolean showCounts = true;
	private boolean showFillLevels = false;
	private boolean flatTop = false;

	public LimitedBarrelBlockEntity() {
		getStorageWrapper().registerUpgradeDefaultsHandler(VoidUpgradeWrapper.class, VOID_UPGRADE_VOIDING_OVERFLOW_OF_EVERYTHING_BY_DEFAULT);
	}

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	@Override
	protected boolean showsCountsAndFillLevels() {
		return true;
	}

	@Override
	public boolean memorizesItemsWhenLocked() {
		return true;
	}

	@Override
	public boolean allowsEmptySlotsMatchingItemInsertsWhenLocked() {
		return false;
	}

	public boolean isFlatTop() {
		return flatTop;
	}

	public void setFlatTop(boolean flatTop) {
		this.flatTop = flatTop;
		markDirty();
		notifyBlockUpdate();
	}

	@Override
	public boolean shouldShowCounts() {
		return showCounts;
	}

	@Override
	public void toggleCountVisibility() {
		showCounts = !showCounts;
		markDirty();
		notifyBlockUpdate();
	}

	@Override
	public List<Integer> getSlotCounts() {
		return getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getSlotCounts();
	}

	@Override
	public boolean shouldShowFillLevels() {
		return showFillLevels;
	}

	@Override
	public void toggleFillLevelVisibility() {
		showFillLevels = !showFillLevels;
		markDirty();
		notifyBlockUpdate();
	}

	@Override
	public List<Float> getSlotFillLevels() {
		return getStorageWrapper().getRenderInfo().getItemDisplayRenderInfo().getSlotFillRatios();
	}

	public float getSlotFillPercentage(int slot) {
		List<Float> fillLevels = getSlotFillLevels();
		return slot > -1 && slot < fillLevels.size() ? fillLevels.get(slot) : 0;
	}

	/** The color of the slot's count, white unless dyed. */
	public int getSlotColor(int slot) {
		return TEXT_COLORS.get(slotColors.getOrDefault(slot, EnumDyeColor.WHITE));
	}

	/** Colors the count of a slot that holds something, or of all of them; takes one dye when any changed. */
	public boolean applyDye(int slot, ItemStack dyeStack, EnumDyeColor dyeColor, boolean applyToAll) {
		InventoryHandler invHandler = getStorageWrapper().getInventoryHandler();
		if (slot < 0 || slot >= invHandler.getSlots()) {
			return false;
		}
		if (applyToAll) {
			boolean success = false;
			for (int i = 0; i < invHandler.getSlots(); i++) {
				success |= applyDye(i, dyeColor, invHandler);
			}
			if (!success) {
				return false;
			}
		} else if (!applyDye(slot, dyeColor, invHandler)) {
			return false;
		}
		dyeStack.shrink(1);
		markDirty();
		notifyBlockUpdate();
		return true;
	}

	private boolean applyDye(int slot, EnumDyeColor dyeColor, InventoryHandler invHandler) {
		if (invHandler.getStackInSlot(slot).isEmpty() || dyeColor == slotColors.get(slot)) {
			return false;
		}
		slotColors.put(slot, dyeColor);
		return true;
	}

	/**
	 * The held stack into the slot; clicked twice quickly, everything of the slot's item from the player's inventory.
	 * Locked, the slot remembers what went in.
	 */
	public boolean depositItem(EntityPlayer player, EnumHand hand, ItemStack stackInHand, int slot) {
		if (world == null) {
			return false;
		}
		long gameTime = world.getTotalWorldTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;

		InventoryHandler invHandler = getStorageWrapper().getInventoryHandler();
		ItemStack stackInSlot = invHandler.getStackInSlot(slot);
		MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);

		if (doubleClick) {
			return depositFromAllOfPlayersInventory(player, slot, invHandler, stackInSlot, memorySettings);
		}
		if (stackInHand.isEmpty()) {
			return false;
		}

		if (stackInSlot.isEmpty()) {
			if (invHandler.isItemValid(slot, stackInHand, player)) {
				int stackLimit = invHandler.getStackLimit(slot, stackInHand);
				invHandler.setStackInSlot(slot, stackInHand.splitStack(stackLimit));
				memorizeIfLocked(slot, memorySettings);
				if (stackInHand.isEmpty()) {
					player.setHeldItem(hand, ItemStack.EMPTY);
				}
				return true;
			}
		} else {
			ItemStack result = invHandler.insertItem(slot, stackInHand, true);
			if (result.getCount() != stackInHand.getCount()) {
				result = invHandler.insertItem(slot, stackInHand, false);
				memorizeIfLocked(slot, memorySettings);
				player.setHeldItem(hand, result);
				return true;
			}
		}
		return false;
	}

	private void memorizeIfLocked(int slot, MemorySettingsCategory memorySettings) {
		if (isLocked()) {
			memorySettings.selectSlot(slot);
			getStorageWrapper().getInventoryHandler().onSlotFilterChanged(slot);
		}
	}

	private boolean depositFromAllOfPlayersInventory(EntityPlayer player, int slot, InventoryHandler invHandler, ItemStack stackInSlot,
			MemorySettingsCategory memorySettings) {
		boolean success = false;
		PlayerMainInvWrapper playerInventory = new PlayerMainInvWrapper(player.inventory);
		for (int playerSlot = 0; playerSlot < playerInventory.getSlots(); playerSlot++) {
			ItemStack playerStack = playerInventory.getStackInSlot(playerSlot);
			if (playerStack.isEmpty()) {
				continue;
			}
			boolean memorized = memorySettings.isSlotSelected(slot) && memorySettings.matchesFilter(slot, playerStack);
			if (stackInSlot.isEmpty() ? memorized || invHandler.isFilterItem(playerStack.getItem()) : ItemHandlerHelper.canItemStacksStack(stackInSlot, playerStack)) {
				ItemStack result = invHandler.insertItem(slot, playerStack, true);
				if (result.getCount() < playerStack.getCount()) {
					ItemStack extracted = playerInventory.extractItem(playerSlot, playerStack.getCount() - result.getCount(), false);
					if (!extracted.isEmpty()) {
						invHandler.insertItem(slot, extracted, false);
						success = true;
					}
				}
			}
		}
		return success;
	}

	/** One of the slot's items, or a stack sneaking, into the player's inventory, or at their feet. */
	public boolean tryToTakeItem(EntityPlayer player, int slot) {
		InventoryHandler inventoryHandler = getStorageWrapper().getInventoryHandler();
		ItemStack stackInSlot = inventoryHandler.getStackInSlot(slot);
		if (stackInSlot.isEmpty() || world == null) {
			return false;
		}
		int countToTake = player.isSneaking() ? Math.min(stackInSlot.getMaxStackSize(), stackInSlot.getCount()) : 1;
		ItemStack stackTaken = inventoryHandler.extractItem(slot, countToTake, false);
		if (player.inventory.addItemStackToInventory(stackTaken)) {
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, .2f, ((world.rand.nextFloat() * 2 - 1) * .7f + 1) * 2);
		}
		if (!stackTaken.isEmpty()) {
			player.dropItem(stackTaken, false);
		}
		return true;
	}

	@Override
	protected void saveSynchronizedData(NBTTagCompound tag) {
		super.saveSynchronizedData(tag);
		if (!showCounts) {
			tag.setBoolean(SHOW_COUNTS_TAG, false);
		}
		if (showFillLevels) {
			tag.setBoolean(SHOW_FILL_LEVELS_TAG, true);
		}
		if (!slotColors.isEmpty()) {
			NBTTagCompound colors = new NBTTagCompound();
			slotColors.forEach((slot, color) -> colors.setInteger(String.valueOf(slot), color.getMetadata()));
			tag.setTag(SLOT_COLORS_TAG, colors);
		}
		if (flatTop) {
			tag.setBoolean(FLAT_TOP_TAG, true);
		}
	}

	@Override
	protected void loadSynchronizedData(NBTTagCompound tag) {
		super.loadSynchronizedData(tag);
		showCounts = !tag.hasKey(SHOW_COUNTS_TAG) || tag.getBoolean(SHOW_COUNTS_TAG);
		showFillLevels = tag.getBoolean(SHOW_FILL_LEVELS_TAG);
		slotColors = new HashMap<>();
		NBTTagCompound colors = tag.getCompoundTag(SLOT_COLORS_TAG);
		for (String key : colors.getKeySet()) {
			try {
				slotColors.put(Integer.valueOf(key), EnumDyeColor.byMetadata(colors.getInteger(key)));
			} catch (NumberFormatException ignored) {
				// not a slot number: left out
			}
		}
		flatTop = tag.getBoolean(FLAT_TOP_TAG);
	}
}
