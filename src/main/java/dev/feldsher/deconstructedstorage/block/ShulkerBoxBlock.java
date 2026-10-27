package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.IntSupplier;

/**
 * A shulker box, facing the way it was placed on. Broken, it drops itself with what it holds, which stays its contents,
 * by UUID; in creative too, as vanilla's does. Gone any other way, as by a command, it spills its items instead of
 * leaving them to no stack.
 */
public class ShulkerBoxBlock extends StorageBlockBase {
	public static final PropertyDirection FACING = PropertyDirection.create("facing");

	private final boolean basicTier;

	public ShulkerBoxBlock(IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier, boolean basicTier) {
		super(Material.ROCK, numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
		this.basicTier = basicTier;
		setHardness(2.0F);
		setResistance(2.0F / 3);
		setSoundType(SoundType.STONE);
		setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
	}

	@Override
	public StorageBlockEntity createTileEntity(World world, IBlockState state) {
		return new ShulkerBoxBlockEntity();
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FACING, EnumFacing.byIndex(meta));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
			EnumHand hand) {
		return getDefaultState().withProperty(FACING, facing);
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
	public boolean causesSuffocation(IBlockState state) {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof ShulkerBoxBlockEntity && !((ShulkerBoxBlockEntity) te).isClosed() ? BlockFaceShape.UNDEFINED : BlockFaceShape.SOLID;
	}

	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileEntity te = source.getTileEntity(pos);
		return te instanceof ShulkerBoxBlockEntity ? ((ShulkerBoxBlockEntity) te).getBoundingBox(state.getValue(FACING)) : FULL_BLOCK_AABB;
	}

	@SuppressWarnings("deprecation")
	@Override
	public EnumPushReaction getPushReaction(IBlockState state) {
		return EnumPushReaction.DESTROY;
	}

	/** As vanilla's: closed, it opens only with room for its lid to rise. */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote || player.isSpectator()) {
			return true;
		}
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof ShulkerBoxBlockEntity) || !canOpen(world, pos, state, (ShulkerBoxBlockEntity) te)) {
			return true;
		}
		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	private static boolean canOpen(World world, BlockPos pos, IBlockState state, ShulkerBoxBlockEntity be) {
		if (be.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
			return true;
		}
		EnumFacing facing = state.getValue(FACING);
		AxisAlignedBB lidBox = FULL_BLOCK_AABB.expand(0.5F * facing.getXOffset(), 0.5F * facing.getYOffset(), 0.5F * facing.getZOffset())
				.contract(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
		return !world.collidesWithAnyBlock(lidBox.offset(pos.offset(facing)));
	}

	/** Picked, a box looks as it does, with nothing in it; broken, it also carries what it holds. */
	@Override
	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		StorageWrapper wrapper = be.getStorageWrapper();
		if (wrapper.hasMainColor()) {
			StorageBlockItem.setMainColor(stack, wrapper.getMainColor());
		}
		if (wrapper.hasAccentColor()) {
			StorageBlockItem.setAccentColor(stack, wrapper.getAccentColor());
		}
	}

	private ItemStack getDropWithContents(IBlockAccess world, BlockPos pos, IBlockState state) {
		ItemStack stack = getStorageStack(world, pos, state);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ShulkerBoxBlockEntity) {
			ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) te;
			StorageWrapper wrapper = be.getStorageWrapper();
			wrapper.getContentsUuid().ifPresent(uuid -> {
				ShulkerBoxItem.setContents(stack, uuid, wrapper.getInventoryHandler().getSlots(), wrapper.getUpgradeHandler().getSlots());
				be.setContentsMovedToItem();
			});
		}
		return stack;
	}

	/** As vanilla's, it drops broken by anything, not only by a pickaxe, as its stone would want. */
	@Override
	public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return true;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		drops.add(getDropWithContents(world, pos, state));
	}

	/** Creative breaking drops nothing: the box drops itself, with what it holds, all the same. */
	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		if (!world.isRemote && player.isCreative()) {
			EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, getDropWithContents(world, pos, state));
			entity.setDefaultPickupDelay();
			world.spawnEntity(entity);
		}
		super.onBlockHarvested(world, pos, state, player);
	}

	/** The contents a stack carries become the box's, at the size the stack says. */
	@Override
	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		super.setPlacedData(stack, be);
		StorageWrapper wrapper = be.getStorageWrapper();
		wrapper.setColors(StorageBlockItem.getMainColor(stack).orElse(-1), StorageBlockItem.getAccentColor(stack).orElse(-1));
		if (be.getWorld() != null && !be.getWorld().isRemote) {
			ShulkerBoxItem.getContentsUuid(stack).ifPresent(uuid -> wrapper.setContents(uuid,
					ShulkerBoxItem.getNumberOfInventorySlots(stack, getNumberOfInventorySlots()), ShulkerBoxItem.getNumberOfUpgradeSlots(stack, getNumberOfUpgradeSlots())));
			NBTTagList pendingItems = ShulkerBoxItem.getPendingItems(stack);
			if (!pendingItems.isEmpty()) {
				InventoryHandler inventoryHandler = wrapper.getInventoryHandler();
				for (int i = 0; i < pendingItems.tagCount(); i++) {
					NBTTagCompound itemTag = pendingItems.getCompoundTagAt(i);
					int slot = itemTag.getByte("Slot") & 255;
					if (slot < inventoryHandler.getSlots()) {
						inventoryHandler.setStackInSlot(slot, new ItemStack(itemTag));
					}
				}
			}
		}
	}

	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));
		if (basicTier) {
			for (EnumDyeColor color : EnumDyeColor.values()) {
				items.add(StorageBlockItem.setColors(new ItemStack(this), color.getColorValue(), color.getColorValue()));
			}
			items.add(StorageBlockItem.setColors(new ItemStack(this), EnumDyeColor.YELLOW.getColorValue(), EnumDyeColor.LIME.getColorValue()));
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

	@SideOnly(Side.CLIENT)
	@Override
	protected TextureAtlasSprite getParticleSprite(StorageBlockEntity be) {
		return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(be.getStorageWrapper().hasMainColor()
				? "minecraft:blocks/shulker_top_white" : "minecraft:blocks/shulker_top_purple");
	}

	@Override
	protected int getParticleColor(StorageBlockEntity be) {
		return be.getStorageWrapper().hasMainColor() ? be.getStorageWrapper().getMainColor() : -1;
	}
}
