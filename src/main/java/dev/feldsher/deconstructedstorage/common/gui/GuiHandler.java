package dev.feldsher.deconstructedstorage.common.gui;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.DecorationTableBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import dev.feldsher.deconstructedstorage.network.StorageContentsMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

/** Opens storage containers, of the storage block at x y z. */
public class GuiHandler implements IGuiHandler {
	public static final int STORAGE = 0;
	public static final int STORAGE_SETTINGS = 1;
	public static final int DECORATION_TABLE = 2;

	/** The decoration table's screen; its block entity goes to the client first, with the colors and parts kept. */
	public static void openDecorationTable(EntityPlayerMP player, BlockPos pos) {
		TileEntity te = player.world.getTileEntity(pos);
		if (!(te instanceof DecorationTableBlockEntity)) {
			return;
		}
		SPacketUpdateTileEntity updatePacket = te.getUpdatePacket();
		if (updatePacket != null) {
			player.connection.sendPacket(updatePacket);
		}
		player.openGui(DeconstructedStorage.instance, DECORATION_TABLE, player.world, pos.getX(), pos.getY(), pos.getZ());
	}

	public static void openStorage(EntityPlayerMP player, BlockPos pos) {
		open(player, pos, STORAGE);
	}

	public static void openSettings(EntityPlayerMP player, BlockPos pos) {
		open(player, pos, STORAGE_SETTINGS);
	}

	/**
	 * The client gets the storage's block entity and contents first, so that it builds the same container: the same
	 * upgrades, and so the same slots, as the server.
	 */
	private static void open(EntityPlayerMP player, BlockPos pos, int id) {
		TileEntity te = player.world.getTileEntity(pos);
		if (!(te instanceof StorageBlockEntity)) {
			return;
		}
		StorageWrapper wrapper = ((StorageBlockEntity) te).getStorageWrapper();
		// contents are created with their UUID the first time the storage is opened
		wrapper.getInventoryHandler();
		wrapper.getContentsUuid().ifPresent(uuid -> PacketHandler.INSTANCE.sendTo(new StorageContentsMessage(uuid, wrapper.getContents().getNbt()), player));
		SPacketUpdateTileEntity updatePacket = te.getUpdatePacket();
		if (updatePacket != null) {
			player.connection.sendPacket(updatePacket);
		}
		player.openGui(DeconstructedStorage.instance, id, player.world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Nullable
	private static StorageBlockEntity getBlockEntity(EntityPlayer player, World world, int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof StorageBlockEntity) || player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > 64.0D) {
			return null;
		}
		return (StorageBlockEntity) te;
	}

	@Nullable
	private static DecorationTableBlockEntity getDecorationTable(EntityPlayer player, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		return te instanceof DecorationTableBlockEntity && player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) <= 64.0D ? (DecorationTableBlockEntity) te : null;
	}

	@Nullable
	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id == DECORATION_TABLE) {
			DecorationTableBlockEntity table = getDecorationTable(player, world, x, y, z);
			return table == null ? null : new DecorationTableMenu(player, table);
		}
		StorageBlockEntity be = getBlockEntity(player, world, x, y, z);
		if (be == null) {
			return null;
		}
		return id == STORAGE_SETTINGS ? new StorageSettingsContainerMenu(player, be) : new StorageContainerMenu(player, be);
	}

	@Nullable
	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		if (id == DECORATION_TABLE) {
			DecorationTableBlockEntity table = getDecorationTable(player, world, x, y, z);
			return table == null ? null : DeconstructedStorage.proxy.createDecorationTableScreen(new DecorationTableMenu(player, table));
		}
		StorageBlockEntity be = getBlockEntity(player, world, x, y, z);
		if (be == null) {
			return null;
		}
		return id == STORAGE_SETTINGS ? DeconstructedStorage.proxy.createStorageSettingsScreen(new StorageSettingsContainerMenu(player, be))
				: DeconstructedStorage.proxy.createStorageScreen(new StorageContainerMenu(player, be));
	}
}
