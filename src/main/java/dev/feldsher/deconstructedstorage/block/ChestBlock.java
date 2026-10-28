package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedstorage.item.ChestBlockItem;
import dev.feldsher.deconstructedstorage.item.StackStorageWrapper;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * A chest, drawn by its block entity's renderer, lid and all. Placed beside another of the same look facing the same
 * way, the two join into a double chest, as vanilla's do; sneaking, a chest joins the one whose side was clicked, or
 * none.
 */
public class ChestBlock extends WoodStorageBlockBase {
	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyEnum<ChestType> TYPE = PropertyEnum.create("type", ChestType.class);
	private static final AxisAlignedBB SINGLE_AABB = new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 14 / 16D, 15 / 16D);
	private static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(1 / 16D, 0, 0, 15 / 16D, 14 / 16D, 15 / 16D);
	private static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 14 / 16D, 1);
	private static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0, 0, 1 / 16D, 15 / 16D, 14 / 16D, 15 / 16D);
	private static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 1, 14 / 16D, 15 / 16D);

	private final boolean basicTier;
	/** While a double chest's halves are swapped for another tier's one by one: the other half isn't gone. */
	private static boolean upgrading = false;

	public ChestBlock(IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier, boolean basicTier) {
		super(numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		this.basicTier = basicTier;
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TYPE, ChestType.SINGLE));
	}

	@Override
	protected boolean isBasicTier() {
		return basicTier;
	}

	@Override
	public StorageBlockEntity createTileEntity(World world, IBlockState state) {
		return new ChestBlockEntity();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, TYPE);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		ChestType[] types = ChestType.values();
		return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(TYPE, types[Math.min(meta >> 2, types.length - 1)]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex() | state.getValue(TYPE).ordinal() << 2;
	}

	public static EnumFacing getConnectedDirection(IBlockState state) {
		return state.getValue(TYPE).getConnectedDirection(state.getValue(FACING));
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
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

	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		if (state.getValue(TYPE) == ChestType.SINGLE) {
			return SINGLE_AABB;
		}
		switch (getConnectedDirection(state)) {
			case SOUTH:
				return SOUTH_AABB;
			case WEST:
				return WEST_AABB;
			case EAST:
				return EAST_AABB;
			default:
				return NORTH_AABB;
		}
	}

	/**
	 * Facing the placer; joined to a chest beside it, of the same look and facing, as vanilla does, or with sneak only
	 * to the one whose side was clicked. Two chests that both have upgrades don't join: one would lose them.
	 */
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clickedFace, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
			EnumHand hand) {
		EnumFacing direction = placer.getHorizontalFacing().getOpposite();
		ItemStack stack = placer.getHeldItem(hand);
		ChestType chestType = ChestType.SINGLE;
		boolean sneaking = placer.isSneaking();
		if (clickedFace.getAxis().isHorizontal() && sneaking) {
			EnumFacing partnerFacing = candidatePartnerFacing(world, pos, clickedFace.getOpposite(), stack);
			if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
				direction = partnerFacing;
				chestType = partnerFacing.rotateYCCW() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
			}
		}
		if (chestType == ChestType.SINGLE && !sneaking) {
			if (direction == candidatePartnerFacing(world, pos, direction.rotateY(), stack)) {
				chestType = ChestType.LEFT;
			} else if (direction == candidatePartnerFacing(world, pos, direction.rotateYCCW(), stack)) {
				chestType = ChestType.RIGHT;
			}
		}
		return getDefaultState().withProperty(FACING, direction).withProperty(TYPE, chestType);
	}

	/** The facing of the single chest that way, when one placed from the stack would join it. */
	@Nullable
	private EnumFacing candidatePartnerFacing(World world, BlockPos pos, EnumFacing direction, ItemStack stack) {
		BlockPos neighborPos = pos.offset(direction);
		IBlockState neighborState = world.getBlockState(neighborPos);
		if (neighborState.getBlock() != this || neighborState.getValue(TYPE) != ChestType.SINGLE) {
			return null;
		}
		TileEntity te = world.getTileEntity(neighborPos);
		if (!(te instanceof ChestBlockEntity)) {
			return null;
		}
		ChestBlockEntity be = (ChestBlockEntity) te;
		StorageWrapper wrapper = be.getStorageWrapper();
		boolean sameLook = StorageBlockItem.getMainColor(stack).orElse(-1) == wrapper.getMainColor()
				&& StorageBlockItem.getAccentColor(stack).orElse(-1) == wrapper.getAccentColor()
				&& WoodStorageBlockItem.getWoodType(stack).orElse(WoodType.DEFAULT) == be.getWoodType().orElse(WoodType.DEFAULT);
		// the half with upgrades becomes the main one: only one of them may have any, or the other's would go
		boolean upgradesFromOneHalf = !hasUpgrades(stack) || InventoryHelper.isEmpty(wrapper.getUpgradeHandler());
		return sameLook && upgradesFromOneHalf ? neighborState.getValue(FACING) : null;
	}

	/** A packed chest's stack carries its upgrades along with its items. */
	private static boolean hasUpgrades(ItemStack stack) {
		return StorageBlockItem.getContentsUuid(stack).isPresent() && !InventoryHelper.isEmpty(StackStorageWrapper.fromStack(stack).getUpgradeHandler());
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		ChestType chestType = state.getValue(TYPE);
		if (chestType == ChestType.SINGLE || world.isRemote) {
			return;
		}
		BlockPos otherPos = pos.offset(getConnectedDirection(state));
		IBlockState otherState = world.getBlockState(otherPos);
		if (otherState.getBlock() != this) {
			return;
		}
		world.setBlockState(otherPos, otherState.withProperty(TYPE, chestType.getOpposite()));
		TileEntity current = world.getTileEntity(pos);
		TileEntity other = world.getTileEntity(otherPos);
		if (current instanceof ChestBlockEntity && other instanceof ChestBlockEntity) {
			joinChests((ChestBlockEntity) current, (ChestBlockEntity) other, chestType);
		}
	}

	/** Runs the swap of a double chest's halves, during which neither half parts from the other. */
	public static void runUpgrading(Runnable upgrade) {
		upgrading = true;
		try {
			upgrade.run();
		} finally {
			upgrading = false;
		}
	}

	/**
	 * Two single chests side by side, facing the same way, made one double chest, as placing one beside the other
	 * would: as vanilla's double chests are upgraded into these.
	 */
	public static void connect(World world, BlockPos pos, BlockPos otherPos) {
		IBlockState state = world.getBlockState(pos);
		IBlockState otherState = world.getBlockState(otherPos);
		EnumFacing facing = state.getValue(FACING);
		EnumFacing toOther = EnumFacing.getFacingFromVector(otherPos.getX() - pos.getX(), 0, otherPos.getZ() - pos.getZ());
		ChestType chestType = toOther == facing.rotateY() ? ChestType.LEFT : ChestType.RIGHT;
		world.setBlockState(pos, state.withProperty(TYPE, chestType));
		world.setBlockState(otherPos, otherState.withProperty(FACING, facing).withProperty(TYPE, chestType.getOpposite()));
		TileEntity current = world.getTileEntity(pos);
		TileEntity other = world.getTileEntity(otherPos);
		if (current instanceof ChestBlockEntity && other instanceof ChestBlockEntity) {
			joinChests((ChestBlockEntity) current, (ChestBlockEntity) other, chestType);
		}
	}

	/** The half with upgrades is the main one; with none, as in 1.21, the left one's partner, the right half. */
	private static void joinChests(ChestBlockEntity current, ChestBlockEntity other, ChestType currentChestType) {
		if (isWithoutUpgrades(current) && (currentChestType == ChestType.LEFT || !isWithoutUpgrades(other))) {
			current.joinWithChest(other);
		} else {
			other.joinWithChest(current);
		}
	}

	private static boolean isWithoutUpgrades(ChestBlockEntity chest) {
		return !chest.getStorageWrapper().getContentsUuid().isPresent() || InventoryHelper.isEmpty(chest.getStorageWrapper().getUpgradeHandler());
	}

	/**
	 * The other half gone, by whatever means, this is a chest of its own again; or, a packed double chest's half whose
	 * contents went with the other into one stack, it goes too, quietly.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		super.neighborChanged(state, world, pos, block, fromPos);
		ChestType chestType = state.getValue(TYPE);
		if (world.isRemote || upgrading || chestType == ChestType.SINGLE || !fromPos.equals(pos.offset(getConnectedDirection(state)))) {
			return;
		}
		IBlockState otherState = world.getBlockState(fromPos);
		if (otherState.getBlock() == this && otherState.getValue(TYPE) == chestType.getOpposite() && otherState.getValue(FACING) == state.getValue(FACING)) {
			return;
		}
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ChestBlockEntity && ((ChestBlockEntity) te).isContentsMovedToItem()) {
			world.setBlockToAir(pos);
			return;
		}
		if (te instanceof ChestBlockEntity) {
			ChestBlockEntity be = (ChestBlockEntity) te;
			if (be.isMainChest()) {
				be.dropSecondPartContents(fromPos);
			} else {
				be.removeDoubleMainPos();
			}
		}
		world.setBlockState(pos, state.withProperty(TYPE, ChestType.SINGLE));
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ChestBlockEntity) {
			((ChestBlockEntity) te).setDestroyedByPlayer();
		}
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	/** As vanilla's: not with a solid block on top, of either half. */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (isBlocked(world, pos) || state.getValue(TYPE) != ChestType.SINGLE && isBlocked(world, pos.offset(getConnectedDirection(state)))) {
			return true;
		}
		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	private static boolean isBlocked(World world, BlockPos pos) {
		return world.getBlockState(pos.up()).isSideSolid(world, pos.up(), EnumFacing.DOWN);
	}

	/** A double chest packs whole: both halves. */
	@Override
	protected void packStorage(World world, BlockPos pos, WoodStorageBlockEntity be) {
		super.packStorage(world, pos, be);
		IBlockState state = world.getBlockState(pos);
		if (state.getValue(TYPE) != ChestType.SINGLE) {
			TileEntity other = world.getTileEntity(pos.offset(getConnectedDirection(state)));
			if (other instanceof ChestBlockEntity) {
				((ChestBlockEntity) other).setPacked(true);
			}
		}
	}

	/** A packed double chest goes as one stack, that places both halves again; the half not broken goes with it. */
	@Override
	protected void addContentsDropData(ItemStack stack, StorageBlockEntity be) {
		IBlockState state = be.getBlockState();
		if (state.getBlock() == this && state.getValue(TYPE) != ChestType.SINGLE) {
			ChestBlockItem.setDoubleChest(stack, true);
			TileEntity other = be.getWorld() == null ? null : be.getWorld().getTileEntity(be.getPos().offset(getConnectedDirection(state)));
			if (other instanceof ChestBlockEntity) {
				((ChestBlockEntity) other).setContentsMovedToItem();
			}
		}
	}

	@Override
	public EnumFacing getFacing(IBlockState state) {
		return state.getValue(FACING);
	}

	@Override
	public List<BlockPos> getNeighborPos(IBlockState state, BlockPos origin, EnumFacing facing) {
		if (state.getValue(TYPE) == ChestType.SINGLE) {
			return Collections.singletonList(origin.offset(facing));
		}
		EnumFacing connectedDirection = getConnectedDirection(state);
		if (connectedDirection == facing) {
			return Collections.singletonList(origin.offset(facing, 2));
		} else if (connectedDirection.getOpposite() == facing) {
			return Collections.singletonList(origin.offset(facing));
		}
		return Arrays.asList(origin.offset(facing), origin.offset(connectedDirection).offset(facing));
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withRotation(mirror.toRotation(state.getValue(FACING)));
	}

	@SideOnly(Side.CLIENT)
	@Override
	protected TextureAtlasSprite getParticleSprite(@Nullable WoodType woodType) {
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(getPlanksTexture(woodType));
	}

	/** The planks of the chest's wood; a tinted one's birch, the palest, to take the tint. */
	private static String getPlanksTexture(@Nullable WoodType woodType) {
		if (woodType == null) {
			return "minecraft:blocks/planks_birch";
		}
		return "minecraft:blocks/planks_" + (woodType == WoodType.DARK_OAK ? "big_oak" : woodType.getName());
	}
}
