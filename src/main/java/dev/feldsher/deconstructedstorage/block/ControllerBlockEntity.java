package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.controller.ControllerBlockEntityBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nullable;

/** A placed controller: its network's inventory, to pipes and to players depositing into it. */
public class ControllerBlockEntity extends ControllerBlockEntityBase {
	private long lastDepositTime = -100;
	@Nullable
	private IItemHandler cachedFailedInsertItemHandler = null;

	/** The held stack, of an item the network has; clicked twice quickly, all such items in the player inventory. */
	public void depositPlayerItems(EntityPlayer player, EnumHand hand) {
		if (world == null) {
			return;
		}
		long gameTime = world.getTotalWorldTime();
		boolean doubleClick = gameTime - lastDepositTime < 10;
		lastDepositTime = gameTime;
		if (doubleClick) {
			PlayerMainInvWrapper playerInventory = new PlayerMainInvWrapper(player.inventory);
			for (int slot = 0; slot < playerInventory.getSlots(); slot++) {
				ItemStack stack = playerInventory.getStackInSlot(slot);
				if (stack.isEmpty() || !canDepositStack(stack)) {
					continue;
				}
				ItemStack resultStack = insertItem(stack, true, false);
				int countToExtract = stack.getCount() - resultStack.getCount();
				if (countToExtract > 0 && playerInventory.extractItem(slot, countToExtract, true).getCount() == countToExtract) {
					insertItem(playerInventory.extractItem(slot, countToExtract, false), false, false);
				}
			}
			return;
		}
		ItemStack itemInHand = player.getHeldItem(hand);
		if (!itemInHand.isEmpty() && canDepositStack(itemInHand)) {
			player.setHeldItem(hand, insertItem(itemInHand, false, false));
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(getExternalItemHandler(facing));
		}
		return super.getCapability(capability, facing);
	}

	/**
	 * Itself without a side; from a side, as to pipes, through a handler that turns away a stack it failed to take this
	 * tick: pipes offer the same stack to each of its many slots, and each try goes through the whole network.
	 */
	public IItemHandler getExternalItemHandler(@Nullable EnumFacing side) {
		if (side == null) {
			return this;
		}
		if (cachedFailedInsertItemHandler == null) {
			cachedFailedInsertItemHandler = new CachedFailedInsertInventoryHandler(() -> this, () -> world != null ? world.getTotalWorldTime() : 0);
		}
		return cachedFailedInsertItemHandler;
	}

	/** The lines to linked blocks are drawn from it, as far as they go. */
	@SideOnly(Side.CLIENT)
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return INFINITE_EXTENT_AABB;
	}
}
