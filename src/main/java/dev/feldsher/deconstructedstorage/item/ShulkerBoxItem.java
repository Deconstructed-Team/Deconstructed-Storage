package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.api.IStashStorageItem;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.StorageBlockContents;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import dev.feldsher.deconstructedstorage.network.RequestStorageContentsMessage;
import dev.feldsher.deconstructedstorage.network.StorageContentsMessage;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A shulker box as an item, one to a stack: broken, a box keeps what it holds, which its stack carries, as a packed
 * storage's does. Items can be stashed into it in any inventory, and destroyed as a dropped item it spills them.
 */
public class ShulkerBoxItem extends StorageBlockItem implements IStashStorageItem {
	private static final long CONTENTS_REQUEST_INTERVAL_MS = 5000;
	private static final Map<UUID, Long> CONTENTS_REQUEST_TIMES = new HashMap<>();

	private static final String PENDING_ITEMS_TAG = "pending_items";

	public ShulkerBoxItem(Block block) {
		super(block);
		setMaxStackSize(1);
	}

	/** Items to go into the box as it's placed, as a vanilla box's crafted into this one: kept as vanilla keeps them. */
	public static void setPendingItems(ItemStack stack, NBTTagList items) {
		if (items.isEmpty()) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag != null) {
				tag.removeTag(PENDING_ITEMS_TAG);
			}
			return;
		}
		StorageNbt.getOrCreateTag(stack).setTag(PENDING_ITEMS_TAG, items);
	}

	@Override
	public boolean hasCustomEntity(ItemStack stack) {
		return true;
	}

	@Nullable
	@Override
	public Entity createEntity(World world, Entity location, ItemStack stack) {
		return location instanceof EntityItem ? new ShulkerBoxItemEntity(world, (EntityItem) location, stack) : null;
	}

	@Override
	public ItemStack stash(ItemStack storageStack, ItemStack stack, boolean simulate) {
		return StackStorageWrapper.fromStack(storageStack).getInventoryForUpgradeProcessing().insertItem(stack, simulate);
	}

	@Override
	public StashResult getItemStashable(ItemStack storageStack, ItemStack stack) {
		StackStorageWrapper wrapper = StackStorageWrapper.fromStack(storageStack);
		Optional<UUID> uuid = wrapper.getContentsUuid();
		// Without contents yet it's empty, once they're made; the client asks for those it hasn't got
		if (!uuid.isPresent() || FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT && !hasClientContents(uuid.get())) {
			return wrapper.isAllowedInStorage(stack) ? StashResult.SPACE : StashResult.NO_SPACE;
		}
		if (wrapper.getInventoryForUpgradeProcessing().insertItem(stack, true).getCount() == stack.getCount()) {
			return StashResult.NO_SPACE;
		}
		InventoryHandler inventory = wrapper.getInventoryHandler();
		for (int slot = 0; slot < inventory.getSlots(); slot++) {
			if (inventory.getStackInSlot(slot).getItem() == stack.getItem()) {
				return StashResult.MATCH_AND_SPACE;
			}
		}
		return wrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class).matchesFilter(stack) ? StashResult.MATCH_AND_SPACE : StashResult.SPACE;
	}

	/** Whether the client has the box's contents; if not, it asks for them, now and then. */
	public static boolean hasClientContents(UUID uuid) {
		if (StorageBlockContents.hasClientContents(uuid)) {
			return true;
		}
		long now = System.currentTimeMillis();
		Long lastRequest = CONTENTS_REQUEST_TIMES.get(uuid);
		if (lastRequest == null || now - lastRequest > CONTENTS_REQUEST_INTERVAL_MS) {
			CONTENTS_REQUEST_TIMES.put(uuid, now);
			PacketHandler.INSTANCE.sendToServer(new RequestStorageContentsMessage(uuid));
		}
		return false;
	}

	/** The client sees the new contents: a box as an item is never open. */
	@Override
	public void onStashed(EntityPlayerMP player, ItemStack storageStack) {
		getContentsUuid(storageStack).ifPresent(uuid ->
				PacketHandler.INSTANCE.sendTo(new StorageContentsMessage(uuid, StorageBlockContents.getContents(uuid).getNbt()), player));
	}

	public static NBTTagList getPendingItems(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag == null ? new NBTTagList() : tag.getTagList(PENDING_ITEMS_TAG, Constants.NBT.TAG_COMPOUND);
	}
}
