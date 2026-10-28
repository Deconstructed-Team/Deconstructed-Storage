package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.controller.ControllerBlockEntityBase;
import dev.feldsher.deconstructedcore.controller.ILinkable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/** A placed storage link: linked, it joins what it's on to the controller; it doesn't join by itself. */
public class StorageLinkBlockEntity extends TileEntity implements ILinkable {
	@Nullable
	private BlockPos controllerPos = null;

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		loadControllerPos(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		saveControllerPos(compound);
		return compound;
	}

	@Override
	public void setControllerPos(BlockPos controllerPos) {
		this.controllerPos = controllerPos;
		markDirty();
	}

	@Override
	public Optional<BlockPos> getControllerPos() {
		return Optional.ofNullable(controllerPos);
	}

	@Override
	public void removeControllerPos() {
		controllerPos = null;
		markDirty();
	}

	@Override
	public BlockPos getStorageBlockPos() {
		return getPos();
	}

	@Override
	public World getStorageBlockLevel() {
		return world;
	}

	@Override
	public boolean canConnectStorages() {
		return true;
	}

	@Override
	public boolean canBeConnected() {
		return false;
	}

	@Override
	public void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getPos());
	}

	@Override
	public void unregisterController() {
		removeControllerPos();
	}

	/** The block it's on. */
	@Override
	public Set<BlockPos> getConnectablePositions() {
		IBlockState state = world.getBlockState(pos);
		EnumFacing facing = state.getBlock() instanceof StorageLinkBlock ? state.getValue(StorageLinkBlock.FACING) : EnumFacing.UP;
		return Collections.singleton(pos.offset(facing.getOpposite()));
	}

	@Override
	public boolean connectLinkedSelf() {
		return false;
	}

	@Override
	public boolean isLinked() {
		return getControllerPos().isPresent();
	}
}
