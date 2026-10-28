package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.client.render.StorageContentsTooltip;
import dev.feldsher.deconstructedstorage.block.StorageBlockContents;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItem;
import dev.feldsher.deconstructedstorage.item.StackStorageWrapper;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import dev.feldsher.deconstructedstorage.network.RequestStorageContentsMessage;
import net.minecraft.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/** A shulker box item's tooltip shows what it holds, with shift held. */
public class ShulkerBoxContentsTooltipSource implements StorageContentsTooltip.IContentsTooltipSource {
	@Override
	public Optional<IStorageWrapper> getStorageWrapper(ItemStack stack) {
		return stack.getItem() instanceof ShulkerBoxItem ? Optional.of(StackStorageWrapper.fromStack(stack)) : Optional.empty();
	}

	@Override
	public boolean hasClientContents(UUID uuid) {
		return StorageBlockContents.hasClientContents(uuid);
	}

	@Override
	public void requestContents(UUID uuid) {
		PacketHandler.INSTANCE.sendToServer(new RequestStorageContentsMessage(uuid));
	}
}
