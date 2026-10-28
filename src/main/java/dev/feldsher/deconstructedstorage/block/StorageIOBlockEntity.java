package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.controller.ControllerBlockEntityBase;
import dev.feldsher.deconstructedcore.controller.ILinkable;
import dev.feldsher.deconstructedcore.inventory.IItemHandlerSimpleInserter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * A placed storage I/O block: in a controller's network, by being next to it or linked to it, pipes reach the
 * network's inventory through it, as through the controller.
 */
public class StorageIOBlockEntity extends TileEntity implements ILinkable {
	private static final String LINKED_TO_CONTROLLER_TAG = "isLinkedToController";
	@Nullable
	private BlockPos controllerPos = null;
	private boolean isLinkedToController = false;

	@Override
	public Set<BlockPos> getConnectablePositions() {
		return Collections.emptySet();
	}

	@Override
	public boolean connectLinkedSelf() {
		return true;
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
	public boolean isLinked() {
		return isLinkedToController && getControllerPos().isPresent();
	}

	@Override
	public void linkToController(BlockPos controllerPos) {
		if (getControllerPos().isPresent()) {
			return;
		}
		isLinkedToController = true;
		ILinkable.super.linkToController(controllerPos);
		markDirty();
	}

	@Override
	public void setNotLinked() {
		isLinkedToController = false;
		removeControllerPos();
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
		return false;
	}

	@Override
	public void addToController(World world, BlockPos pos, BlockPos controllerPos) {
		ControllerBlockEntityBase.getLoadedTileEntity(world, controllerPos, ControllerBlockEntityBase.class).ifPresent(c -> c.addStorage(pos));
	}

	@Override
	public boolean canBeConnected() {
		return isLinked() || !getControllerPos().isPresent();
	}

	@Override
	public void registerController(ControllerBlockEntityBase controllerBlockEntity) {
		setControllerPos(controllerBlockEntity.getPos());
	}

	@Override
	public void unregisterController() {
		removeControllerPos();
	}

	public void removeFromController() {
		if (world != null && !world.isRemote) {
			if (isLinked()) {
				unlinkFromController();
				return;
			}
			getControllerPos().flatMap(p -> ControllerBlockEntityBase.getLoadedTileEntity(world, p, ControllerBlockEntityBase.class))
					.ifPresent(c -> c.removeNonConnectingBlock(getPos()));
			removeControllerPos();
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		loadControllerPos(compound);
		isLinkedToController = compound.getBoolean(LINKED_TO_CONTROLLER_TAG);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		saveControllerPos(compound);
		if (isLinkedToController) {
			compound.setBoolean(LINKED_TO_CONTROLLER_TAG, true);
		}
		return compound;
	}

	/** The controller's inventory as pipes see it from that side; none outside of a network. */
	@Nullable
	protected IItemHandler getControllerItemHandler(@Nullable EnumFacing side) {
		return getControllerPos().flatMap(p -> ControllerBlockEntityBase.getLoadedTileEntity(world, p, ControllerBlockEntity.class))
				.map(controller -> controller.getExternalItemHandler(side)).orElse(null);
	}

	@Nullable
	protected IItemHandler getExternalItemHandler(@Nullable EnumFacing side) {
		return getControllerItemHandler(side);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && getControllerPos().isPresent() || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			IItemHandler handler = getExternalItemHandler(facing);
			if (handler != null) {
				return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
			}
		}
		return super.getCapability(capability, facing);
	}

	/** Takes items in as one slot, which hands nothing out: pipes then look at one slot, not all of the network. */
	public static class Input extends StorageIOBlockEntity {
		@Nullable
		@Override
		protected IItemHandler getExternalItemHandler(@Nullable EnumFacing side) {
			IItemHandler handler = getControllerItemHandler(null);
			return handler instanceof IItemHandlerSimpleInserter ? new SingleSlotInput((IItemHandlerSimpleInserter) handler) : null;
		}
	}

	/** Hands items out of all of the network, takes nothing in. */
	public static class Output extends StorageIOBlockEntity {
		@Nullable
		@Override
		protected IItemHandler getExternalItemHandler(@Nullable EnumFacing side) {
			IItemHandler handler = getControllerItemHandler(null);
			return handler == null ? null : new OutputOnly(handler);
		}
	}

	private static class SingleSlotInput implements IItemHandler {
		private final IItemHandlerSimpleInserter itemHandler;

		SingleSlotInput(IItemHandlerSimpleInserter itemHandler) {
			this.itemHandler = itemHandler;
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return itemHandler.insertItem(stack, simulate);
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 99;
		}
	}

	private static class OutputOnly implements IItemHandler {
		private final IItemHandler itemHandler;

		OutputOnly(IItemHandler itemHandler) {
			this.itemHandler = itemHandler;
		}

		@Override
		public int getSlots() {
			return itemHandler.getSlots();
		}

		@Nonnull
		@Override
		public ItemStack getStackInSlot(int slot) {
			return itemHandler.getStackInSlot(slot);
		}

		@Nonnull
		@Override
		public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
			return stack;
		}

		@Nonnull
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			return itemHandler.extractItem(slot, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return itemHandler.getSlotLimit(slot);
		}
	}
}
