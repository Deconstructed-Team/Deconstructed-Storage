package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedcore.common.gui.StorageContainerMenuBase;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import dev.feldsher.deconstructedstorage.block.WoodStorageBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/** A placed storage's container. Opened, the storage counts it, and shows it, as a barrel its open top. */
public class StorageContainerMenu extends StorageContainerMenuBase<StorageWrapper> {
	private final StorageBlockEntity storageBlockEntity;

	public StorageContainerMenu(EntityPlayer player, StorageBlockEntity storageBlockEntity) {
		super(player, storageBlockEntity.getStorageWrapper(), -1, false);
		this.storageBlockEntity = storageBlockEntity;
		storageBlockEntity.startOpen(player);
	}

	public StorageBlockEntity getStorageBlockEntity() {
		return storageBlockEntity;
	}

	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		storageBlockEntity.stopOpen(player);
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		BlockPos pos = storageBlockEntity.getPos();
		TileEntity te = player.world.getTileEntity(pos);
		return te == storageBlockEntity && !te.isInvalid() && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
				&& !(te instanceof WoodStorageBlockEntity && ((WoodStorageBlockEntity) te).isPacked());
	}

	/** Locked, a storage whose slots remember what they hold, as a limited barrel, remembers what's put in through here too. */
	@Override
	public void detectAndSendChanges() {
		if (!player.world.isRemote && storageBlockEntity.isLocked() && storageBlockEntity.memorizesItemsWhenLocked()) {
			MemorySettingsCategory memorySettings = getStorageWrapper().getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
			for (int slot = 0; slot < getNumberOfStorageInventorySlots(); slot++) {
				if (!memorySettings.isSlotSelected(slot) && !getSlot(slot).getStack().isEmpty()) {
					memorySettings.selectSlot(slot);
					getStorageWrapper().getInventoryHandler().onSlotFilterChanged(slot);
				}
			}
		}
		super.detectAndSendChanges();
	}

	@Override
	public Optional<BlockPos> getBlockPosition() {
		return Optional.of(storageBlockEntity.getPos());
	}

	@Override
	public Optional<Entity> getEntity() {
		return Optional.empty();
	}

	@Override
	protected void openSettingsScreen() {
		if (player instanceof EntityPlayerMP) {
			GuiHandler.openSettings((EntityPlayerMP) player, storageBlockEntity.getPos());
		}
	}

	@Override
	protected void onUpgradeChanged() {
		storageBlockEntity.refreshTickableUpgrades();
		if (player instanceof EntityPlayerMP) {
			ContainerReopener.scheduleReopen((EntityPlayerMP) player, this);
		}
	}
}
