package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedcore.common.gui.IServerUpdater;
import dev.feldsher.deconstructedstorage.block.VerticalFacing;
import dev.feldsher.deconstructedstorage.upgrades.IOMode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** What an upgrade does on each side of the storage, set from its tab by the side as seen from the front. */
public class SideIOContainer {
	private static final String DATA_SIDE = "side";
	private static final String DATA_IO_MODE = "ioMode";
	private final Supplier<EnumFacing> horizontalDirection;
	private final Supplier<VerticalFacing> verticalFacing;
	private final Function<EnumFacing, IOMode> getDirectionIOMode;
	private final BiConsumer<EnumFacing, IOMode> setDirectionIOMode;
	private final boolean canSideIOBeDisabled;
	private final IServerUpdater serverUpdater;

	public SideIOContainer(IServerUpdater serverUpdater, Supplier<EnumFacing> horizontalDirection, Supplier<VerticalFacing> verticalFacing,
			Function<EnumFacing, IOMode> getDirectionIOMode, BiConsumer<EnumFacing, IOMode> setDirectionIOMode, boolean canSideIOBeDisabled) {
		this.serverUpdater = serverUpdater;
		this.horizontalDirection = horizontalDirection;
		this.verticalFacing = verticalFacing;
		this.getDirectionIOMode = getDirectionIOMode;
		this.setDirectionIOMode = setDirectionIOMode;
		this.canSideIOBeDisabled = canSideIOBeDisabled;
	}

	public boolean handlePacket(NBTTagCompound data) {
		if (data.hasKey(DATA_IO_MODE)) {
			setSideIO(BlockSide.fromName(data.getString(DATA_SIDE)), IOMode.fromName(data.getString(DATA_IO_MODE)));
			return true;
		}
		return false;
	}

	public void toggleSideIO(BlockSide side) {
		IOMode next = getSideIOMode(side).next();
		if (!canSideIOBeDisabled && next == IOMode.DISABLED) {
			next = next.next();
		}
		setSideIO(side, next);
	}

	private void setSideIO(BlockSide side, IOMode ioMode) {
		setDirectionIOMode.accept(toDirection(side), ioMode);
		serverUpdater.sendDataToServer(() -> {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString(DATA_SIDE, side.getSerializedName());
			tag.setString(DATA_IO_MODE, ioMode.getSerializedName());
			return tag;
		});
	}

	public EnumFacing toDirection(BlockSide side) {
		return side.toDirection(horizontalDirection.get(), verticalFacing.get());
	}

	public IOMode getSideIOMode(BlockSide side) {
		return getDirectionIOMode.apply(toDirection(side));
	}
}
