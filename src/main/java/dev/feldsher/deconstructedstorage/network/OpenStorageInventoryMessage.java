package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.common.gui.GuiHandler;
import dev.feldsher.deconstructedstorage.common.gui.StorageSettingsContainerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** From a storage's settings, back to the storage: the one whose settings are open, so no position comes from the client. */
public class OpenStorageInventoryMessage implements IMessage {
	@Override
	public void fromBytes(ByteBuf buf) {
		//noop
	}

	@Override
	public void toBytes(ByteBuf buf) {
		//noop
	}

	public static class Handler implements IMessageHandler<OpenStorageInventoryMessage, IMessage> {
		@Override
		public IMessage onMessage(OpenStorageInventoryMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				if (player.openContainer instanceof StorageSettingsContainerMenu) {
					StorageSettingsContainerMenu menu = (StorageSettingsContainerMenu) player.openContainer;
					if (menu.canInteractWith(player)) {
						GuiHandler.openStorage(player, menu.getPos());
					}
				}
			});
			return null;
		}
	}
}
