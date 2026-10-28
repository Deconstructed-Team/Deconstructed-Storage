package dev.feldsher.deconstructedstorage.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * A storage I/O block, or one only for input or output: placed next to a controller network, or linked to one, it
 * gives pipes the network's inventory. It joins, but doesn't connect storages beyond it.
 */
public class StorageIOBlock extends Block {
	private final Supplier<StorageIOBlockEntity> blockEntityFactory;

	public StorageIOBlock(Supplier<StorageIOBlockEntity> blockEntityFactory) {
		super(Material.ROCK);
		this.blockEntityFactory = blockEntityFactory;
		setHardness(3F);
		// as 1.21's 6: 1.12 keeps three times what it's given
		setResistance(2F);
		setSoundType(SoundType.STONE);
		setHarvestLevel("pickaxe", 0);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		ControllerBlock.addTooltip(this, tooltip);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return blockEntityFactory.get();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		TileEntity te = world.getTileEntity(pos);
		if (!world.isRemote && te instanceof StorageIOBlockEntity) {
			((StorageIOBlockEntity) te).addToAdjacentController();
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof StorageIOBlockEntity) {
			((StorageIOBlockEntity) te).removeFromController();
		}
		super.breakBlock(world, pos, state);
	}
}
