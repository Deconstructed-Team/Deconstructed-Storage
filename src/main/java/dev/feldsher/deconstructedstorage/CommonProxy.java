package dev.feldsher.deconstructedstorage;

import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import dev.feldsher.deconstructedstorage.common.gui.DecorationTableMenu;

/** What only one side does. The server's half does nothing; the client's is {@link dev.feldsher.deconstructedstorage.client.ClientProxy}. */
public class CommonProxy {
	public void preInit() {
		//noop
	}

	public void init() {
		//noop
	}

	@Nullable
	public Object createStorageScreen(StorageContainerMenu container) {
		return null;
	}

	@Nullable
	public Object createDecorationTableScreen(DecorationTableMenu container) {
		return null;
	}

	public Object createStorageSettingsScreen(StorageSettingsContainerMenu container) {
		return null;
	}

	/** On the client, bits of the storage at pos, where an entity landed on it. */
	public void addLandingParticles(BlockPos pos, double x, double y, double z, int count) {
		//noop
	}
}
