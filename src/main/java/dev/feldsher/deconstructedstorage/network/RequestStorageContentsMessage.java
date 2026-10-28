package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.block.StorageBlockContents;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

/**
 * The client asks for what a shulker box item holds, to show it or what can be stashed into it. Sent back only for a
 * box the player has at hand: on the cursor or in a slot of the open container.
 */
public class RequestStorageContentsMessage implements IMessage {
	private UUID storageUuid = new UUID(0, 0);

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public RequestStorageContentsMessage() {
	}

	public RequestStorageContentsMessage(UUID storageUuid) {
		this.storageUuid = storageUuid;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		storageUuid = new UUID(buf.readLong(), buf.readLong());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(storageUuid.getMostSignificantBits());
		buf.writeLong(storageUuid.getLeastSignificantBits());
	}

	public static class Handler implements IMessageHandler<RequestStorageContentsMessage, IMessage> {
		@Override
		public IMessage onMessage(RequestStorageContentsMessage message, MessageContext ctx) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(() -> {
				if (isAtHand(player, message.storageUuid)) {
					PacketHandler.INSTANCE.sendTo(new StorageContentsMessage(message.storageUuid,
							StorageBlockContents.getContents(message.storageUuid).getNbt()), player);
				}
			});
			return null;
		}

		private static boolean isAtHand(EntityPlayerMP player, UUID uuid) {
			if (hasUuid(player.inventory.getItemStack(), uuid)) {
				return true;
			}
			for (Slot slot : player.openContainer.inventorySlots) {
				if (hasUuid(slot.getStack(), uuid)) {
					return true;
				}
			}
			return false;
		}

		private static boolean hasUuid(ItemStack stack, UUID uuid) {
			return stack.getItem() instanceof ShulkerBoxItem && StorageBlockItem.getContentsUuid(stack).map(uuid::equals).orElse(false);
		}
	}
}
