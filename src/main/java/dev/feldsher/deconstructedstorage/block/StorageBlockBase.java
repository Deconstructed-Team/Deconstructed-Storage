package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.common.gui.GuiHandler;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;

/**
 * A storage block. Broken, it drops its items and upgrades, and itself as an item that keeps how it looks; its
 * contents then go. The block entity has to be there when the drop is made, which 1.12 takes away before it asks for
 * drops: {@link #removedByPlayer} leaves it, and {@link #harvestBlock} removes the block after the drop.
 */
public abstract class StorageBlockBase extends Block implements IStorageBlock {
	private final IntSupplier numberOfInventorySlotsSupplier;
	private final IntSupplier numberOfUpgradeSlotsSupplier;

	protected StorageBlockBase(Material material, IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier) {
		super(material);
		this.numberOfInventorySlotsSupplier = numberOfInventorySlotsSupplier;
		this.numberOfUpgradeSlotsSupplier = numberOfUpgradeSlotsSupplier;
		setHardness(2.5F);
		setResistance(2.5F);
		setSoundType(SoundType.WOOD);
	}

	@Override
	public int getNumberOfInventorySlots() {
		return numberOfInventorySlotsSupplier.getAsInt();
	}

	@Override
	public int getNumberOfUpgradeSlots() {
		return numberOfUpgradeSlotsSupplier.getAsInt();
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public abstract StorageBlockEntity createTileEntity(World world, IBlockState state);

	@Nullable
	protected static StorageBlockEntity getStorageBlockEntity(IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		return te instanceof StorageBlockEntity ? (StorageBlockEntity) te : null;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		if (getStorageBlockEntity(world, pos) != null && player instanceof EntityPlayerMP) {
			GuiHandler.openStorage((EntityPlayerMP) player, pos);
		}
		return true;
	}

	/** The stack this storage drops as, and is picked as: how it looks, with no contents. */
	public ItemStack getStorageStack(IBlockAccess world, BlockPos pos, IBlockState state) {
		ItemStack stack = new ItemStack(this);
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be != null) {
			addDropData(stack, be);
		}
		return stack;
	}

	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		if (be.hasCustomName()) {
			stack.setStackDisplayName(be.getStorageDisplayName());
		}
	}

	/**
	 * On both sides: the client places the block before the server tells it about it, and would draw it plain till
	 * then. How it looks comes from the stack alone, and makes no contents.
	 */
	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be != null) {
			if (stack.hasDisplayName()) {
				be.setCustomName(stack.getDisplayName());
			}
			setPlacedData(stack, be);
		}
	}

	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		//noop by default
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, @Nullable RayTraceResult target, World world, BlockPos pos, @Nullable EntityPlayer player) {
		return getStorageStack(world, pos, state);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		drops.add(getStorageStack(world, pos, state));
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		// harvested, the block goes in harvestBlock, after the drop was made from its block entity
		return willHarvest || super.removedByPlayer(state, world, pos, player, false);
	}

	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
		super.harvestBlock(world, player, pos, state, te, tool);
		world.setBlockToAir(pos);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be != null) {
			be.dropContents();
			world.updateComparatorOutputLevel(pos, this);
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		return be == null ? 0 : ItemHandlerHelper.calcRedstoneFromInventory(be.getStorageWrapper().getInventoryForInputOutput());
	}

	@Override
	public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
		TileEntity te = world.getTileEntity(pos);
		return te != null && te.receiveClientEvent(id, param);
	}
}
