package dev.feldsher.deconstructedstorage.upgrades.hopper;

import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerBase;
import dev.feldsher.deconstructedcore.common.gui.UpgradeContainerType;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogicContainer;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.block.VerticalFacing;
import dev.feldsher.deconstructedstorage.common.gui.SideIOContainer;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.upgrades.IOMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public class HopperUpgradeContainer extends UpgradeContainerBase<HopperUpgradeWrapper, HopperUpgradeContainer> {
	public static final String EMPTY_INPUT_FILTER_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_input_filter_slot";
	public static final String EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND = DeconstructedStorage.MOD_ID + ":item/empty_output_filter_slot";
	private final ContentsFilterLogicContainer inputFilterLogicContainer;
	private final ContentsFilterLogicContainer outputFilterLogicContainer;
	private final SideIOContainer sideIOContainer;

	@Nullable
	private EnumFacing horizontalDirection;
	@Nullable
	private VerticalFacing verticalFacing;

	public HopperUpgradeContainer(EntityPlayer player, int upgradeContainerId, HopperUpgradeWrapper upgradeWrapper, UpgradeContainerType<HopperUpgradeWrapper, HopperUpgradeContainer> type) {
		super(player, upgradeContainerId, upgradeWrapper, type);

		inputFilterLogicContainer = new ContentsFilterLogicContainer(() -> this.upgradeWrapper.getInputFilterLogic(), this,
				slot -> addFilterSlot(slot, EMPTY_INPUT_FILTER_SLOT_BACKGROUND));
		outputFilterLogicContainer = new ContentsFilterLogicContainer(() -> this.upgradeWrapper.getOutputFilterLogic(), this,
				slot -> addFilterSlot(slot, EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND));

		sideIOContainer = new SideIOContainer(this, this::getHorizontalDirection, this::getVerticalFacing, this::getDirectionIOMode, this::setDirectionIOMode, false);
	}

	private void addFilterSlot(Slot slot, String background) {
		slot.setBackgroundName(background);
		slots.add(slot);
	}

	private EnumFacing getHorizontalDirection() {
		if (horizontalDirection == null) {
			initBlockRotation();
		}
		return horizontalDirection;
	}

	private VerticalFacing getVerticalFacing() {
		if (verticalFacing == null) {
			initBlockRotation();
		}
		return verticalFacing;
	}

	/** Not in the constructor: the storage's container, which says where the storage is, isn't the player's open one yet. */
	private void initBlockRotation() {
		horizontalDirection = EnumFacing.NORTH;
		verticalFacing = VerticalFacing.NO;
		if (player.openContainer instanceof StorageContainerMenu) {
			((StorageContainerMenu) player.openContainer).getBlockPosition().ifPresent(pos -> {
				IBlockState state = player.world.getBlockState(pos);
				if (state.getBlock() instanceof StorageBlockBase) {
					StorageBlockBase storageBlock = (StorageBlockBase) state.getBlock();
					horizontalDirection = storageBlock.getHorizontalDirection(state);
					verticalFacing = storageBlock.getVerticalFacing(state);
				}
			});
		}
	}

	private void setDirectionIOMode(EnumFacing direction, IOMode ioMode) {
		upgradeWrapper.setPullingFrom(direction, ioMode == IOMode.PULL || ioMode == IOMode.PUSH_PULL);
		upgradeWrapper.setPushingTo(direction, ioMode == IOMode.PUSH || ioMode == IOMode.PUSH_PULL);
	}

	private IOMode getDirectionIOMode(EnumFacing direction) {
		boolean pulling = upgradeWrapper.isPullingFrom(direction);
		boolean pushing = upgradeWrapper.isPushingTo(direction);
		if (pulling && pushing) {
			return IOMode.PUSH_PULL;
		} else if (pulling) {
			return IOMode.PULL;
		} else if (pushing) {
			return IOMode.PUSH;
		}
		return IOMode.OFF;
	}

	@Override
	public void handlePacket(NBTTagCompound data) {
		inputFilterLogicContainer.handlePacket(data);
		outputFilterLogicContainer.handlePacket(data);
		sideIOContainer.handlePacket(data);
	}

	public ContentsFilterLogicContainer getInputFilterLogicContainer() {
		return inputFilterLogicContainer;
	}

	public ContentsFilterLogicContainer getOutputFilterLogicContainer() {
		return outputFilterLogicContainer;
	}

	public SideIOContainer getSideIOContainer() {
		return sideIOContainer;
	}
}
