package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.block.StorageBlockContents;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A storage's contents, for the client's copy. Sent before the storage's container opens, so that the client builds
 * it with the same upgrades, and so the same slots, as the server.
 */
public class StorageContentsMessage implements IMessage {
	@Nullable
	private UUID storageUuid;
	@Nullable
	private NBTTagCompound contents;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public StorageContentsMessage() {
	}

	public StorageContentsMessage(UUID storageUuid, NBTTagCompound contents) {
		this.storageUuid = storageUuid;
		this.contents = contents;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		storageUuid = new UUID(buf.readLong(), buf.readLong());
		contents = ByteBufUtils.readTag(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		if (storageUuid == null) {
			throw new IllegalStateException("No storage UUID to send");
		}
		buf.writeLong(storageUuid.getMostSignificantBits());
		buf.writeLong(storageUuid.getLeastSignificantBits());
		ByteBufUtils.writeTag(buf, contents);
	}

	public static class Handler implements IMessageHandler<StorageContentsMessage, IMessage> {
		@Override
		public IMessage onMessage(StorageContentsMessage message, MessageContext ctx) {
			UUID uuid = message.storageUuid;
			NBTTagCompound contents = message.contents;
			if (uuid != null && contents != null) {
				FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> StorageBlockContents.setClientContents(uuid, contents));
			}
			return null;
		}
	}
}
