package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.block.ChestBlockEntity;
import dev.feldsher.deconstructedstorage.block.ChestType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A chest as an item; packed from a double chest, both its halves, which it places again side by side, the placed one
 * the main half and the other to its left, from where the player looks.
 */
public class ChestBlockItem extends WoodStorageBlockItem {
	private static final String DOUBLE_CHEST_TAG = "double_chest";

	public ChestBlockItem(Block block) {
		super(block);
	}

	public static boolean isDoubleChest(ItemStack stack) {
		NBTTagCompound tag = stack.getTagCompound();
		return tag != null && tag.getBoolean(DOUBLE_CHEST_TAG);
	}

	public static void setDoubleChest(ItemStack stack, boolean doubleChest) {
		if (doubleChest) {
			StorageNbt.getOrCreateTag(stack).setBoolean(DOUBLE_CHEST_TAG, true);
		} else if (stack.getTagCompound() != null) {
			stack.getTagCompound().removeTag(DOUBLE_CHEST_TAG);
		}
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
		if (!isDoubleChest(stack) || !(newState.getBlock() instanceof ChestBlock)) {
			return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
		}
		EnumFacing facing = newState.getValue(ChestBlock.FACING);
		BlockPos otherPos = pos.offset(ChestType.RIGHT.getConnectedDirection(facing));
		if (!world.getBlockState(otherPos).getBlock().isReplaceable(world, otherPos) || !world.getWorldBorder().contains(otherPos)) {
			return false;
		}
		IBlockState mainState = newState.withProperty(ChestBlock.TYPE, ChestType.RIGHT);
		if (!super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, mainState)) {
			return false;
		}
		if (!world.isRemote) {
			world.setBlockState(otherPos, mainState.withProperty(ChestBlock.TYPE, ChestType.LEFT));
			TileEntity other = world.getTileEntity(otherPos);
			if (other instanceof ChestBlockEntity) {
				ChestBlockEntity otherChest = (ChestBlockEntity) other;
				otherChest.getStorageWrapper().setColors(StorageBlockItem.getMainColor(stack).orElse(-1), StorageBlockItem.getAccentColor(stack).orElse(-1));
				otherChest.setWoodType(WoodStorageBlockItem.getWoodType(stack).orElse(null));
				otherChest.setMainPos(pos);
			}
		}
		return true;
	}
}
