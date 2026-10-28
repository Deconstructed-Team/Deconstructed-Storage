package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sneaking with an upgrade, a right click on a storage's front puts in as many as it takes, and on a controller as
 * many as each storage of the network takes. 1.12 doesn't activate a block for a sneaking player with an item. Only
 * the server knows whether they go in: the client sends the click on as it is.
 */
@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID)
public final class StorageSneakInteractions {
	private StorageSneakInteractions() {}

	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		World world = event.getWorld();
		if (world.isRemote || !player.isSneaking() || event.getItemStack().isEmpty()) {
			return;
		}
		IBlockState state = world.getBlockState(event.getPos());
		TileEntity te = world.getTileEntity(event.getPos());
		boolean applied = false;
		if (state.getBlock() instanceof StorageBlockBase && te instanceof StorageBlockEntity) {
			applied = event.getFace() == ((StorageBlockBase) state.getBlock()).getFacing(state)
					&& StorageBlockBase.tryFillUpgrades(player, event.getHand(), (StorageBlockEntity) te);
		} else if (state.getBlock() instanceof ControllerBlock && te instanceof ControllerBlockEntity) {
			applied = ControllerBlock.applyToStorages((ControllerBlockEntity) te, player, event.getHand(), true);
		}
		if (applied) {
			event.setCanceled(true);
			event.setCancellationResult(EnumActionResult.SUCCESS);
		}
	}
}
