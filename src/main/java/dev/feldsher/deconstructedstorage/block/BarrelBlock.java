package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.function.IntSupplier;

/**
 * A barrel, facing any way. Its facing and flat top are its metadata; whether it's open, and its wood and colors,
 * come from its block entity, as 1.12 has no room in the metadata for them, and go to its model as the render data.
 */
public class BarrelBlock extends WoodStorageBlockBase {
	public static final PropertyDirection FACING = PropertyDirection.create("facing");
	public static final PropertyBool FLAT_TOP = PropertyBool.create("flat_top");
	public static final IUnlistedProperty<BarrelRenderData> RENDER_DATA = new BarrelRenderData.Property();

	private final boolean basicTier;

	public BarrelBlock(IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier, boolean basicTier) {
		super(numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		this.basicTier = basicTier;
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(FLAT_TOP, false));
	}

	@Override
	protected boolean isBasicTier() {
		return basicTier;
	}

	@Override
	public StorageBlockEntity createTileEntity(World world, IBlockState state) {
		return new BarrelBlockEntity();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[] {FACING, FLAT_TOP}, new IUnlistedProperty[] {RENDER_DATA});
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta & 7)).withProperty(FLAT_TOP, (meta & 8) != 0);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex() | (state.getValue(FLAT_TOP) ? 8 : 0);
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!(state instanceof IExtendedBlockState) || !(be instanceof WoodStorageBlockEntity)) {
			return state;
		}
		return ((IExtendedBlockState) state).withProperty(RENDER_DATA, BarrelRenderData.of((WoodStorageBlockEntity) be));
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
			EnumHand hand) {
		return getDefaultState().withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer))
				.withProperty(FLAT_TOP, BarrelBlockItem.isFlatTop(placer.getHeldItem(hand)));
	}

	@Override
	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		if (be.getWorld() != null) {
			IBlockState state = be.getWorld().getBlockState(be.getPos());
			if (state.getBlock() == this && state.getValue(FLAT_TOP)) {
				BarrelBlockItem.setFlatTop(stack, true);
			}
		}
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withRotation(mirror.toRotation(state.getValue(FACING)));
	}

	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}

	public EnumFacing getFacing(IBlockState state) {
		return state.getValue(FACING);
	}
}
