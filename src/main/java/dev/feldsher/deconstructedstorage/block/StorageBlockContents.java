package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.storage.StorageContents;
import dev.feldsher.deconstructedcore.storage.StorageContentsManager;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * The contents of every storage block, by its UUID; the block carries only the UUID. On the server they're saved a
 * file per storage, in the world's {@code data/deconstructedstorage}, not in the chunk as 1.21 keeps them: 1.12 can't
 * save a chunk over a megabyte, and a few full storages with stack upgrades get there. The client has those the server
 * sent it.
 */
public final class StorageBlockContents {
	private static final StorageContentsManager CONTENTS = StorageContentsManager.create(DeconstructedStorage.MOD_ID);

	private StorageBlockContents() {}

	/** The storage's contents, the same live ones for the block and the stack it's packed into. */
	public static StorageContents getContents(UUID storageUuid) {
		return CONTENTS.getOrCreate(storageUuid);
	}

	public static void setClientContents(UUID storageUuid, NBTTagCompound contents) {
		CONTENTS.setClientContents(storageUuid, contents);
	}

	public static void removeContents(UUID storageUuid) {
		CONTENTS.remove(storageUuid);
	}
}
