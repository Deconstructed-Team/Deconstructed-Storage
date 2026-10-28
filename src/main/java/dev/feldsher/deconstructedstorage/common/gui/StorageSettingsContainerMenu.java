package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedcore.common.gui.SettingsContainerMenu;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class StorageSettingsContainerMenu extends SettingsContainerMenu<StorageWrapper> {
	private final StorageBlockEntity storageBlockEntity;

	public StorageSettingsContainerMenu(EntityPlayer player, StorageBlockEntity storageBlockEntity) {
		super(player, storageBlockEntity.getStorageWrapper());
		this.storageBlockEntity = storageBlockEntity;
	}

	public StorageBlockEntity getStorageBlockEntity() {
		return storageBlockEntity;
	}

	public BlockPos getPos() {
		return storageBlockEntity.getPos();
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		BlockPos pos = storageBlockEntity.getPos();
		TileEntity te = player.world.getTileEntity(pos);
		return te == storageBlockEntity && !te.isInvalid() && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}
}
