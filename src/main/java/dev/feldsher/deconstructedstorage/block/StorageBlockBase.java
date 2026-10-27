package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.client.render.StorageParticles;
import dev.feldsher.deconstructedstorage.common.gui.GuiHandler;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import dev.feldsher.deconstructedcore.upgrades.IPickupResponseUpgrade;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.util.math.AxisAlignedBB;
import java.util.List;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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
import java.util.function.BiConsumer;
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
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be != null && player instanceof EntityPlayerMP) {
			GuiHandler.openStorage((EntityPlayerMP) player, be.getMainBlockEntity().getPos());
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
			if (placer != null && placer.getHeldItemOffhand().getItem() instanceof StorageToolItem) {
				StorageToolItem.useOffHandOnPlaced(placer.getHeldItemOffhand(), be);
			}
		}
	}

	/** The contents a stack carries become the storage's, at the size the stack says. */
	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		if (be.getWorld() != null && !be.getWorld().isRemote) {
			StorageBlockItem.getContentsUuid(stack).ifPresent(uuid -> be.getStorageWrapper().setContents(uuid,
					StorageBlockItem.getNumberOfInventorySlots(stack, getNumberOfInventorySlots()), StorageBlockItem.getNumberOfUpgradeSlots(stack, getNumberOfUpgradeSlots())));
		}
	}

	/**
	 * Whether it goes as a stack that carries what it holds, as a shulker box always does and a packed storage: its
	 * contents stay, by UUID, and the stack takes them along. Else its items drop, and its contents go.
	 */
	protected boolean dropsWithContents(StorageBlockEntity be) {
		return false;
	}

	/** The stack with the contents of the storage this block is part of; they're then the stack's, not the block's to drop. */
	protected ItemStack getDropWithContents(IBlockAccess world, BlockPos pos, IBlockState state) {
		ItemStack stack = getStorageStack(world, pos, state);
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be != null && dropsWithContents(be)) {
			StorageBlockEntity main = be.getMainBlockEntity();
			StorageWrapper wrapper = main.getStorageWrapper();
			wrapper.getContentsUuid().ifPresent(uuid -> {
				StorageBlockItem.setContents(stack, uuid, wrapper.getInventoryHandler().getSlots(), wrapper.getUpgradeHandler().getSlots());
				addContentsDropData(stack, be);
				main.setContentsMovedToItem();
				be.setContentsMovedToItem();
			});
		}
		return stack;
	}

	/** What else a stack with contents says, as that it's both halves of a double chest. */
	protected void addContentsDropData(ItemStack stack, StorageBlockEntity be) {
		//noop by default
	}

	@Override
	public ItemStack getPickBlock(IBlockState state, @Nullable RayTraceResult target, World world, BlockPos pos, @Nullable EntityPlayer player) {
		return getStorageStack(world, pos, state);
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		drops.add(getDropWithContents(world, pos, state));
	}

	/** Creative breaking drops nothing: one that goes with what it holds drops itself all the same, not to lose it. */
	@Override
	public void onBlockHarvested(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!world.isRemote && player.isCreative() && be != null && dropsWithContents(be)) {
			EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, getDropWithContents(world, pos, state));
			entity.setDefaultPickupDelay();
			world.spawnEntity(entity);
		}
		super.onBlockHarvested(world, pos, state, player);
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
			be.setRemovedState(state);
			be.dropContents();
			world.updateComparatorOutputLevel(pos, this);
		}
		super.breakBlock(world, pos, state);
	}

	/**
	 * Items get a hair smaller box to land on, as 1.21 gives them, so that one lying on the storage is inside its
	 * block: the pickup upgrades take it in then.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
			@Nullable Entity entity, boolean isActualState) {
		if (!(entity instanceof EntityItem)) {
			super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState);
			return;
		}
		AxisAlignedBB box = state.getCollisionBoundingBox(world, pos);
		if (box != null && box != NULL_AABB) {
			addCollisionBoxToList(pos, entityBox, collidingBoxes, box.shrink(0.05 / 16));
		}
	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
		if (world.isRemote || !(entity instanceof EntityItem) || entity.isDead) {
			return;
		}
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (be == null || !be.getMainStorageWrapper().getContentsUuid().isPresent()) {
			return;
		}
		EntityItem itemEntity = (EntityItem) entity;
		ItemStack remaining = itemEntity.getItem().copy();
		for (IPickupResponseUpgrade upgrade : be.getMainStorageWrapper().getUpgradeHandler().getWrappersThatImplement(IPickupResponseUpgrade.class)) {
			remaining = upgrade.pickup(world, remaining, false);
			if (remaining.isEmpty()) {
				break;
			}
		}
		if (remaining.getCount() < itemEntity.getItem().getCount()) {
			if (remaining.isEmpty()) {
				itemEntity.setDead();
			} else {
				itemEntity.setItem(remaining);
			}
		}
	}

	@Override
	public boolean hasComparatorInputOverride(IBlockState state) {
		return true;
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		return be == null ? 0 : ItemHandlerHelper.calcRedstoneFromInventory(be.getMainStorageWrapper().getInventoryForInputOutput());
	}

	/** What its bits look like when broken, hit or run over; null leaves them to the model's particle texture. */
	@SideOnly(Side.CLIENT)
	@Nullable
	protected TextureAtlasSprite getParticleSprite(StorageBlockEntity be) {
		return null;
	}

	/** The tint of its bits, or -1 for none. */
	protected int getParticleColor(StorageBlockEntity be) {
		return -1;
	}

	@SideOnly(Side.CLIENT)
	private boolean addParticles(IBlockAccess world, BlockPos pos, BiConsumer<TextureAtlasSprite, Integer> add) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		TextureAtlasSprite sprite = be == null ? null : getParticleSprite(be);
		if (sprite == null) {
			return false;
		}
		add.accept(sprite, getParticleColor(be));
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
		IBlockState state = world.getBlockState(pos);
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addDestroyEffects(world, pos, state, sprite, color, manager));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
		BlockPos pos = target.getBlockPos();
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addHitEffects(world, pos, target.sideHit, state, sprite, color, manager));
	}

	/** The server has nothing to show; the client throws bits of what the storage is made of. */
	@Override
	public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
		return world.isRemote && addRunningParticles(state, world, pos, entity);
	}

	@SideOnly(Side.CLIENT)
	private boolean addRunningParticles(IBlockState state, World world, BlockPos pos, Entity entity) {
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addRunningEffects(world, pos, state, entity, sprite, color));
	}

	@Override
	public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
		TileEntity te = world.getTileEntity(pos);
		return te != null && te.receiveClientEvent(id, param);
	}
}
