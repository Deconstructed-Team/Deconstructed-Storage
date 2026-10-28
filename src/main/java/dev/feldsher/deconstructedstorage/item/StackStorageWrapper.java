package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.inventory.StorageWrapperRepository;
import dev.feldsher.deconstructedstorage.block.ShulkerBoxBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * A storage carried as an item, as a shulker box with what it holds: its contents by the UUID on the stack, with the
 * stack's slot counts. What changes on it, a UUID made as items first go in, is written back to the stack.
 */
public class StackStorageWrapper extends StorageWrapper {
	private static final String UUID_TAG = "uuid";
	private final ItemStack storageStack;

	private StackStorageWrapper(ItemStack storageStack) {
		super(() -> {});
		this.storageStack = storageStack;
		NBTTagCompound tag = new NBTTagCompound();
		StorageBlockItem.getContentsUuid(storageStack).ifPresent(uuid -> tag.setUniqueId(UUID_TAG, uuid));
		tag.setInteger(NUMBER_OF_INVENTORY_SLOTS_TAG, StorageBlockItem.getNumberOfInventorySlots(storageStack, 0));
		tag.setInteger(NUMBER_OF_UPGRADE_SLOTS_TAG, StorageBlockItem.getNumberOfUpgradeSlots(storageStack, -1));
		StorageBlockItem.getMainColor(storageStack).ifPresent(color -> tag.setInteger(MAIN_COLOR_TAG, color));
		StorageBlockItem.getAccentColor(storageStack).ifPresent(color -> tag.setInteger(ACCENT_COLOR_TAG, color));
		load(tag);
	}

	public static StackStorageWrapper fromStack(ItemStack stack) {
		return StorageWrapperRepository.getStorageWrapper(stack, StackStorageWrapper.class, StackStorageWrapper::new);
	}

	@Override
	protected void save() {
		getContentsUuid().ifPresent(uuid -> {
			StorageBlockItem.setContents(storageStack, uuid, getNumberOfInventorySlots(), getNumberOfUpgradeSlots());
			movePendingItemsIn();
		});
	}

	/**
	 * A vanilla box's items, crafted into this one, go into the contents as soon as it has them: kept apart, placing
	 * the box would put them over what went in since.
	 */
	private void movePendingItemsIn() {
		if (!(storageStack.getItem() instanceof ShulkerBoxItem)) {
			return;
		}
		NBTTagList pendingItems = ShulkerBoxItem.getPendingItems(storageStack);
		if (pendingItems.isEmpty()) {
			return;
		}
		ShulkerBoxItem.setPendingItems(storageStack, new NBTTagList());
		NBTTagList left = new NBTTagList();
		for (int i = 0; i < pendingItems.tagCount(); i++) {
			NBTTagCompound itemTag = pendingItems.getCompoundTagAt(i);
			ItemStack remainder = getInventoryHandler().insertItem(new ItemStack(itemTag), false);
			if (!remainder.isEmpty()) {
				NBTTagCompound remainderTag = remainder.writeToNBT(new NBTTagCompound());
				remainderTag.setByte("Slot", itemTag.getByte("Slot"));
				left.appendTag(remainderTag);
			}
		}
		ShulkerBoxItem.setPendingItems(storageStack, left);
	}

	@Override
	public int getDefaultNumberOfInventorySlots() {
		Block block = Block.getBlockFromItem(storageStack.getItem());
		return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getNumberOfInventorySlots() : 0;
	}

	@Override
	public int getDefaultNumberOfUpgradeSlots() {
		Block block = Block.getBlockFromItem(storageStack.getItem());
		return block instanceof StorageBlockBase ? ((StorageBlockBase) block).getNumberOfUpgradeSlots() : 0;
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return storageStack.getItem() instanceof ShulkerBoxItem && ShulkerBoxBlockEntity.isAllowedInShulkerBox(stack);
	}

	@Override
	public ItemStack getWrappedStorageStack() {
		return storageStack;
	}

	/** Only told in a storage's screen, which a box as an item has none of. */
	@Override
	public String getStorageType() {
		return ShulkerBoxBlockEntity.STORAGE_TYPE;
	}

	@Override
	public String getDisplayName() {
		return storageStack.getDisplayName();
	}
}
