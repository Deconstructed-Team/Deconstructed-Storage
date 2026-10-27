package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** The player scrolled with the storage tool in the main hand, sneaking: the tool goes to its next or previous mode. */
public class ScrolledToolMessage implements IMessage {
	private boolean next;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public ScrolledToolMessage() {
	}

	public ScrolledToolMessage(boolean next) {
		this.next = next;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		next = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(next);
	}

	public static class Handler implements IMessageHandler<ScrolledToolMessage, IMessage> {
		@Override
		public IMessage onMessage(ScrolledToolMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				ItemStack stack = player.getHeldItemMainhand();
				if (stack.getItem() instanceof StorageToolItem) {
					StorageToolItem.cycleMode(stack, message.next);
				}
			});
			return null;
		}
	}
}
