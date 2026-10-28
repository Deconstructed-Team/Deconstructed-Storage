package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.util.ColorHelper;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What a limited barrel's front does that its block isn't asked about: in creative, a left click on it takes items
 * out instead of breaking the barrel; sneaking with a dye, a right click colors all its counts, as 1.12 doesn't
 * activate a block for a sneaking player with an item. In survival, the block's own left click takes items out.
 */
@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public final class LimitedBarrelInteractions {
	private LimitedBarrelInteractions() {}

	@SubscribeEvent
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		if (!player.isCreative()) {
			return;
		}
		World world = event.getWorld();
		IBlockState state = world.getBlockState(event.getPos());
		if (!(state.getBlock() instanceof LimitedBarrelBlock)) {
			return;
		}
		LimitedBarrelBlock barrel = (LimitedBarrelBlock) state.getBlock();
		if (!barrel.isLookingAtFront(world, event.getPos(), state, player)) {
			return;
		}
		if (!world.isRemote) {
			barrel.tryToTakeItem(world, event.getPos(), state, player);
		}
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		if (!player.isSneaking() || event.getFace() == null) {
			return;
		}
		World world = event.getWorld();
		IBlockState state = world.getBlockState(event.getPos());
		if (!(state.getBlock() instanceof LimitedBarrelBlock) || event.getFace() != ((LimitedBarrelBlock) state.getBlock()).getFacing(state)) {
			return;
		}
		if (ColorHelper.getDyeColor(event.getItemStack()) == null) {
			return;
		}
		if (!world.isRemote) {
			((LimitedBarrelBlock) state.getBlock()).tryToDyeAll(world, event.getPos(), state, event.getFace(), event.getItemStack());
		}
		event.setCanceled(true);
		event.setCancellationResult(EnumActionResult.SUCCESS);
	}
}
