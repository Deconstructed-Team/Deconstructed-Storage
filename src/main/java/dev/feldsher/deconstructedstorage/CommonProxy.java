package dev.feldsher.deconstructedstorage;

import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;

import javax.annotation.Nullable;

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
	public Object createDecorationTableScreen(dev.feldsher.deconstructedstorage.common.gui.DecorationTableMenu container) {
		return null;
	}

	public Object createStorageSettingsScreen(StorageSettingsContainerMenu container) {
		return null;
	}
}
