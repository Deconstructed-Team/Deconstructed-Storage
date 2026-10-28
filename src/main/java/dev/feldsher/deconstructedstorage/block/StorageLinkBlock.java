package dev.feldsher.deconstructedstorage.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A storage link: a plate on a storage that the storage tool links to a controller within its range, joining the
 * storage, and those joined to it, to that controller's network. It faces away from the block it's on.
 */
public class StorageLinkBlock extends Block {
	public static final PropertyDirection FACING = PropertyDirection.create("facing");
	private static final Map<EnumFacing, AxisAlignedBB> SHAPES = new EnumMap<>(EnumFacing.class);

	static {
		double px = 1 / 16D;
		SHAPES.put(EnumFacing.UP, new AxisAlignedBB(px, 0, px, 1 - px, 2 * px, 1 - px));
		SHAPES.put(EnumFacing.DOWN, new AxisAlignedBB(px, 1 - 2 * px, px, 1 - px, 1, 1 - px));
		SHAPES.put(EnumFacing.SOUTH, new AxisAlignedBB(px, px, 0, 1 - px, 1 - px, 2 * px));
		SHAPES.put(EnumFacing.NORTH, new AxisAlignedBB(px, px, 1 - 2 * px, 1 - px, 1 - px, 1));
		SHAPES.put(EnumFacing.EAST, new AxisAlignedBB(0, px, px, 2 * px, 1 - px, 1 - px));
		SHAPES.put(EnumFacing.WEST, new AxisAlignedBB(1 - 2 * px, px, px, 1, 1 - px, 1 - px));
	}

	public StorageLinkBlock() {
		super(Material.ROCK);
		setHardness(2.5F);
		// as 1.21's 5: 1.12 keeps three times what it's given
		setResistance(5F / 3);
		setSoundType(SoundType.STONE);
		setHarvestLevel("pickaxe", 0);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		ControllerBlock.addTooltip(this, tooltip);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	/** On the face clicked; on a link facing this way, the other way, so that two go back to back. */
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
			EnumHand hand) {
		IBlockState behind = world.getBlockState(pos.offset(facing.getOpposite()));
		boolean backToBack = behind.getBlock() == this && behind.getValue(FACING) == facing;
		return getDefaultState().withProperty(FACING, backToBack ? facing.getOpposite() : facing);
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withRotation(mirror.toRotation(state.getValue(FACING)));
	}

	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return SHAPES.get(state.getValue(FACING));
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new StorageLinkBlockEntity();
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof StorageLinkBlockEntity) {
			((StorageLinkBlockEntity) te).unlinkFromController();
		}
		super.breakBlock(world, pos, state);
	}
}
