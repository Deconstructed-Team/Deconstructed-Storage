package dev.feldsher.deconstructedstorage.network;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * An entity landed on a storage: the clients near throw bits of what it's made of, where it landed. 1.12's own landing
 * dust takes the model's one particle texture, which can't know the wood or the colors.
 */
public class LandingParticlesMessage implements IMessage {
	private BlockPos pos = BlockPos.ORIGIN;
	private double x;
	private double y;
	private double z;
	private int count;

	@SuppressWarnings("unused") // instantiated by SimpleNetworkWrapper
	public LandingParticlesMessage() {
	}

	public LandingParticlesMessage(BlockPos pos, double x, double y, double z, int count) {
		this.pos = pos;
		this.x = x;
		this.y = y;
		this.z = z;
		this.count = count;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		pos = BlockPos.fromLong(buf.readLong());
		x = buf.readDouble();
		y = buf.readDouble();
		z = buf.readDouble();
		count = buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(pos.toLong());
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeShort(Math.min(count, Short.MAX_VALUE));
	}

	public static class Handler implements IMessageHandler<LandingParticlesMessage, IMessage> {
		@Override
		public IMessage onMessage(LandingParticlesMessage message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() ->
					DeconstructedStorage.proxy.addLandingParticles(message.pos, message.x, message.y, message.z, message.count));
			return null;
		}
	}
}
