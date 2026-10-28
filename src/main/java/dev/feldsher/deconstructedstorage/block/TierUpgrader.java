package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.upgrades.stack.StackUpgradeItem;
import dev.feldsher.deconstructedstorage.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityShulkerBox;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Swaps a placed storage for another tier's, in place, keeping all it holds and how it looks: one of these storages, or
 * vanilla's chest or shulker box. The new block's entity takes the old one's data, its contents by UUID among them, and
 * its inventory grows to the new tier's size.
 */
public final class TierUpgrader {
	/** Chests that are vanilla's, or other mods' kept as vanilla's, with the wood each is of: oak for vanilla's. */
	private static final Map<Block, Function<TileEntity, WoodType>> VANILLA_CHESTS = new HashMap<>();

	static {
		registerVanillaChest(Blocks.CHEST, te -> WoodType.OAK);
	}

	private TierUpgrader() {}

	/** Another mod's chest that is vanilla's {@link BlockChest} and {@link TileEntityChest}, made of the wood its block entity says. */
	public static void registerVanillaChest(Block block, Function<TileEntity, WoodType> getWoodType) {
		VANILLA_CHESTS.put(block, getWoodType);
	}

	public static boolean isVanillaChest(Block block) {
		return VANILLA_CHESTS.containsKey(block);
	}

	/** How many upgrade items it takes: both halves of a double chest are upgraded, with one each. */
	public static int getCountRequired(World world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			return 2;
		}
		return isVanillaChest(state.getBlock()) && getVanillaChestPartner(world, pos) != null ? 2 : 1;
	}

	/**
	 * The block it becomes upgraded to that tier: vanilla's chest and shulker box only to begin with, from the basic
	 * tier; these storages only from the one tier.
	 */
	@Nullable
	public static Block getUpgradedBlock(Block block, @Nullable StorageTier from, StorageTier to) {
		if (from == null || from == StorageTier.BASIC) {
			if (isVanillaChest(block)) {
				return ModBlocks.getInTier(ModBlocks.CHEST, to).orElse(null);
			}
			if (block instanceof BlockShulkerBox) {
				return ModBlocks.getInTier(ModBlocks.SHULKER_BOX, to).orElse(null);
			}
		}
		if (from != null && ModBlocks.getTier(block).map(tier -> tier == from).orElse(false)) {
			return ModBlocks.getInTier(block, to).orElse(null);
		}
		return null;
	}

	/** Whether it's open, or otherwise can't be swapped now. */
	public static boolean isUpgradingBlocked(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof StorageBlockEntity) {
			StorageBlockEntity main = ((StorageBlockEntity) te).getMainBlockEntity();
			return main.isOpen() || main instanceof WoodStorageBlockEntity && ((WoodStorageBlockEntity) main).isPacked();
		}
		if (te instanceof TileEntityChest) {
			return ((TileEntityChest) te).numPlayersUsing > 0;
		}
		return te instanceof TileEntityShulkerBox && ((TileEntityShulkerBox) te).getAnimationStatus() != TileEntityShulkerBox.AnimationStatus.CLOSED;
	}

	/** One of these storages to the same kind in a higher tier; a double chest's two halves both. */
	public static boolean upgradeStorage(World world, BlockPos pos, Block newBlock) {
		IBlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			BlockPos otherPos = pos.offset(ChestBlock.getConnectedDirection(state));
			ChestBlock.runUpgrading(() -> {
				swap(world, pos, newBlock);
				swap(world, otherPos, newBlock);
			});
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof StorageBlockEntity) {
				growToTier(((StorageBlockEntity) te).getMainBlockEntity(), 2);
			}
			return true;
		}
		swap(world, pos, newBlock);
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof StorageBlockEntity) {
			growToTier((StorageBlockEntity) te, 1);
		}
		return true;
	}

	private static void swap(World world, BlockPos pos, Block newBlock) {
		IBlockState state = world.getBlockState(pos);
		TileEntity oldTe = world.getTileEntity(pos);
		if (!(oldTe instanceof StorageBlockEntity)) {
			return;
		}
		StorageBlockEntity oldBe = (StorageBlockEntity) oldTe;
		NBTTagCompound tag = oldBe.writeToNBT(new NBTTagCompound());
		oldBe.setBeingUpgraded();
		world.setBlockState(pos, copyProperties(state, newBlock.getDefaultState()), 3);
		TileEntity newTe = world.getTileEntity(pos);
		if (newTe instanceof StorageBlockEntity) {
			newTe.readFromNBT(tag);
			newTe.markDirty();
		}
	}

	private static void growToTier(StorageBlockEntity be, int sizeMultiplier) {
		Block block = be.getBlockState().getBlock();
		if (!(block instanceof StorageBlockBase) || !be.getStorageWrapper().getContentsUuid().isPresent()) {
			return;
		}
		StorageBlockBase storageBlock = (StorageBlockBase) block;
		StorageWrapper wrapper = be.getStorageWrapper();
		int inventorySlots = wrapper.getInventoryHandler().getSlots();
		int upgradeSlots = wrapper.getUpgradeHandler().getSlots();
		wrapper.changeSize(Math.max(0, storageBlock.getNumberOfInventorySlots() * sizeMultiplier - inventorySlots),
				Math.max(0, storageBlock.getNumberOfUpgradeSlots() - upgradeSlots));
		// a limited barrel a tier up holds more in each slot
		wrapper.getInventoryHandler().setBaseSlotLimit(StackUpgradeItem.getInventorySlotLimit(wrapper));
		be.refreshTickableUpgrades();
		be.notifyBlockUpdate();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static IBlockState copyProperties(IBlockState from, IBlockState to) {
		for (IProperty property : from.getPropertyKeys()) {
			if (to.getPropertyKeys().contains(property)) {
				to = to.withProperty(property, from.getValue(property));
			}
		}
		return to;
	}

	/** Vanilla's chest to one of these: of its wood, with its items in the same slots; a double one as both halves. */
	public static boolean upgradeVanillaChest(World world, BlockPos pos, Block newBlock) {
		BlockPos partner = getVanillaChestPartner(world, pos);
		ChestBlock.runUpgrading(() -> {
			swapVanillaChest(world, pos, newBlock);
			if (partner != null) {
				swapVanillaChest(world, partner, newBlock);
			}
		});
		if (partner != null) {
			ChestBlock.connect(world, pos, partner);
		}
		return true;
	}

	/** The other half: the same block next to it, of the same wood, as other mods' chests join only so. */
	@Nullable
	private static BlockPos getVanillaChestPartner(World world, BlockPos pos) {
		Block block = world.getBlockState(pos).getBlock();
		WoodType woodType = getVanillaChestWoodType(world, pos);
		for (EnumFacing facing : EnumFacing.HORIZONTALS) {
			BlockPos partner = pos.offset(facing);
			if (world.getBlockState(partner).getBlock() == block && getVanillaChestWoodType(world, partner) == woodType) {
				return partner;
			}
		}
		return null;
	}

	@Nullable
	private static WoodType getVanillaChestWoodType(World world, BlockPos pos) {
		Function<TileEntity, WoodType> getWoodType = VANILLA_CHESTS.get(world.getBlockState(pos).getBlock());
		TileEntity te = world.getTileEntity(pos);
		return getWoodType == null || te == null ? null : getWoodType.apply(te);
	}

	private static void swapVanillaChest(World world, BlockPos pos, Block newBlock) {
		IBlockState state = world.getBlockState(pos);
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityChest)) {
			return;
		}
		TileEntityChest chest = (TileEntityChest) te;
		WoodType woodType = getVanillaChestWoodType(world, pos);
		NonNullList<ItemStack> items = NonNullList.withSize(chest.getSizeInventory(), ItemStack.EMPTY);
		for (int slot = 0; slot < items.size(); slot++) {
			items.set(slot, chest.removeStackFromSlot(slot));
		}
		String customName = chest.hasCustomName() ? chest.getName() : null;
		world.setBlockState(pos, newBlock.getDefaultState().withProperty(ChestBlock.FACING, state.getValue(BlockChest.FACING)), 3);
		TileEntity newTe = world.getTileEntity(pos);
		if (newTe instanceof ChestBlockEntity) {
			ChestBlockEntity be = (ChestBlockEntity) newTe;
			be.setWoodType(woodType == null ? WoodType.OAK : woodType);
			be.setCustomName(customName);
			fill(be, items);
		}
	}

	/** Vanilla's shulker box to one of these: tinted its color, but purple, the plain look, with its items in the same slots. */
	public static boolean upgradeVanillaShulkerBox(World world, BlockPos pos, Block newBlock) {
		IBlockState state = world.getBlockState(pos);
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityShulkerBox)) {
			return false;
		}
		TileEntityShulkerBox box = (TileEntityShulkerBox) te;
		NonNullList<ItemStack> items = NonNullList.withSize(box.getSizeInventory(), ItemStack.EMPTY);
		for (int slot = 0; slot < items.size(); slot++) {
			items.set(slot, box.removeStackFromSlot(slot));
		}
		String customName = box.hasCustomName() ? box.getName() : null;
		// cleared, it drops no box of itself as it goes
		box.clear();
		EnumDyeColor color = ((BlockShulkerBox) state.getBlock()).getColor();
		world.setBlockState(pos, newBlock.getDefaultState().withProperty(ShulkerBoxBlock.FACING, state.getValue(BlockShulkerBox.FACING)), 3);
		TileEntity newTe = world.getTileEntity(pos);
		if (newTe instanceof ShulkerBoxBlockEntity) {
			ShulkerBoxBlockEntity be = (ShulkerBoxBlockEntity) newTe;
			if (color != EnumDyeColor.PURPLE) {
				be.getStorageWrapper().setColors(color.getColorValue(), color.getColorValue());
			}
			be.setCustomName(customName);
			fill(be, items);
		}
		return true;
	}

	private static void fill(StorageBlockEntity be, NonNullList<ItemStack> items) {
		InventoryHandler inventoryHandler = be.getStorageWrapper().getInventoryHandler();
		for (int slot = 0; slot < items.size() && slot < inventoryHandler.getSlots(); slot++) {
			if (!items.get(slot).isEmpty()) {
				inventoryHandler.setStackInSlot(slot, items.get(slot));
			}
		}
		be.notifyBlockUpdate();
	}
}
