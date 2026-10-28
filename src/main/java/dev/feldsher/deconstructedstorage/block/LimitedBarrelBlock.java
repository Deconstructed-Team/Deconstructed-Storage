package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.settings.SettingsHandler;
import dev.feldsher.deconstructedcore.settings.itemdisplay.ItemDisplaySettingsCategory;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.settings.nosort.NoSortSettingsCategory;
import dev.feldsher.deconstructedcore.util.ColorHelper;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.IntSupplier;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.init.ModItems;
import net.minecraft.client.particle.ParticleManager;

/**
 * A barrel of one to four slots, each holding many stacks of one item: its base stack size multiplier says how many.
 * Its front shows the slots' items, counts and fill levels. A click on the front puts the held item into the slot
 * clicked, a quick second one all of that item the player has; a left click takes one out, sneaking a stack. A dye
 * colors the slot's count; sneaking, all of them. Its side, or the front while sneaking, opens it.
 * <p>
 * It faces a side, or up or down turned toward a side, which is how its slots are laid out then: that's twelve ways,
 * and 1.12 has sixteen states a block, so its flat top is kept by its block entity, not in its state.
 */
public class LimitedBarrelBlock extends BarrelBlock {
	public static final PropertyDirection HORIZONTAL_FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyEnum<VerticalFacing> VERTICAL_FACING = PropertyEnum.create("vertical_facing", VerticalFacing.class);
	private final int numberOfInventorySlots;
	private final IntSupplier baseStackSizeMultiplierSupplier;

	public LimitedBarrelBlock(int numberOfInventorySlots, IntSupplier baseStackSizeMultiplierSupplier, IntSupplier numberOfUpgradeSlotsSupplier, boolean basicTier) {
		super(() -> numberOfInventorySlots, numberOfUpgradeSlotsSupplier, basicTier);
		this.numberOfInventorySlots = numberOfInventorySlots;
		this.baseStackSizeMultiplierSupplier = baseStackSizeMultiplierSupplier;
	}

	@Override
	protected IBlockState createDefaultState() {
		return blockState.getBaseState().withProperty(HORIZONTAL_FACING, EnumFacing.NORTH).withProperty(VERTICAL_FACING, VerticalFacing.NO).withProperty(FLAT_TOP, false);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[] {HORIZONTAL_FACING, VERTICAL_FACING, FLAT_TOP}, new IUnlistedProperty[] {RENDER_DATA});
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(HORIZONTAL_FACING, EnumFacing.byHorizontalIndex(meta & 3))
				.withProperty(VERTICAL_FACING, VerticalFacing.values()[Math.min(meta >> 2, VerticalFacing.values().length - 1)]);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(HORIZONTAL_FACING).getHorizontalIndex() | state.getValue(VERTICAL_FACING).ordinal() << 2;
	}

	@SuppressWarnings("deprecation")
	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		return state.withProperty(FLAT_TOP, be instanceof LimitedBarrelBlockEntity && ((LimitedBarrelBlockEntity) be).isFlatTop());
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
			EnumHand hand) {
		return getDefaultState().withProperty(HORIZONTAL_FACING, placer.getHorizontalFacing().getOpposite())
				.withProperty(VERTICAL_FACING, VerticalFacing.fromDirection(EnumFacing.getDirectionFromEntityLiving(pos, placer)));
	}

	@Override
	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		super.setPlacedData(stack, be);
		if (be instanceof LimitedBarrelBlockEntity) {
			((LimitedBarrelBlockEntity) be).setFlatTop(BarrelBlockItem.isFlatTop(stack));
		}
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!world.isRemote && be != null) {
			setupDefaultSettings(be.getStorageWrapper(), numberOfInventorySlots);
		}
	}

	/** It shows every slot, keeps them out of sorting, and remembers items with their NBT. */
	public static void setupDefaultSettings(IStorageWrapper storageWrapper, int numberOfInventorySlots) {
		SettingsHandler settingsHandler = storageWrapper.getSettingsHandler();
		ItemDisplaySettingsCategory itemDisplaySettings = settingsHandler.getTypeCategory(ItemDisplaySettingsCategory.class);
		for (int slot = 0; slot < numberOfInventorySlots; slot++) {
			itemDisplaySettings.selectSlot(slot);
		}
		settingsHandler.getTypeCategory(NoSortSettingsCategory.class).selectSlots(0, numberOfInventorySlots);
		settingsHandler.getTypeCategory(MemorySettingsCategory.class).setIgnoreNbt(false);
	}

	@Override
	public StorageBlockEntity createTileEntity(World world, IBlockState state) {
		return new LimitedBarrelBlockEntity();
	}

	@Override
	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		if (be instanceof LimitedBarrelBlockEntity && ((LimitedBarrelBlockEntity) be).isFlatTop()) {
			BarrelBlockItem.setFlatTop(stack, true);
		}
	}

	@Override
	public EnumFacing getFacing(IBlockState state) {
		VerticalFacing verticalFacing = state.getValue(VERTICAL_FACING);
		return verticalFacing == VerticalFacing.NO ? state.getValue(HORIZONTAL_FACING) : verticalFacing.getDirection();
	}

	@Override
	public VerticalFacing getVerticalFacing(IBlockState state) {
		return state.getValue(VERTICAL_FACING);
	}

	@Override
	public EnumFacing getHorizontalDirection(IBlockState state) {
		return state.getValue(HORIZONTAL_FACING);
	}

	/** Which way the top of its front looks: up, for one facing a side; for one facing up or down, along its horizontal facing. */
	public static EnumFacing getFrontUp(IBlockState state) {
		EnumFacing horizontalFacing = state.getValue(HORIZONTAL_FACING);
		switch (state.getValue(VERTICAL_FACING)) {
			case UP:
				return horizontalFacing.getOpposite();
			case DOWN:
				return horizontalFacing;
			default:
				return EnumFacing.UP;
		}
	}

	/** Which way the right of its front looks, as seen by one standing before it. */
	public static EnumFacing getFrontRight(IBlockState state) {
		EnumFacing horizontalFacing = state.getValue(HORIZONTAL_FACING);
		return state.getValue(VERTICAL_FACING) == VerticalFacing.NO ? horizontalFacing.rotateYCCW() : horizontalFacing.getOpposite().rotateY();
	}

	@Override
	public IBlockState withRotation(IBlockState state, Rotation rotation) {
		return state.withProperty(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING)));
	}

	@Override
	public IBlockState withMirror(IBlockState state, Mirror mirror) {
		return state.withRotation(mirror.toRotation(state.getValue(HORIZONTAL_FACING)));
	}

	@Override
	public int getBaseStackSizeMultiplier() {
		return baseStackSizeMultiplierSupplier.getAsInt();
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		String key = numberOfInventorySlots == 1 ? "limited_barrel_singular" : "limited_barrel_plural";
		tooltip.add(TextFormatting.DARK_GRAY + StorageTranslationHelper.INSTANCE.translate("block." + DeconstructedStorage.MOD_ID + "." + key + ".tooltip",
				numberOfInventorySlots, getBaseStackSizeMultiplier()));
		super.addInformation(stack, world, tooltip, flag);
	}

	/**
	 * On the front, not sneaking, the held item goes into the slot clicked, or a dye colors its count; that's all the
	 * front does then, the rest opens it.
	 */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
			float hitZ) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!(be instanceof LimitedBarrelBlockEntity) || facing != getFacing(state) || player.isSneaking()
				|| player.getHeldItem(hand).getItem() == ModItems.PACKING_TAPE) {
			return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
		}
		LimitedBarrelBlockEntity barrel = (LimitedBarrelBlockEntity) be;
		if (world.isRemote || barrel.isPacked()) {
			return true;
		}
		int slot = getInteractionSlot(state, hitX, hitY, hitZ);
		ItemStack stackInHand = player.getHeldItem(hand);
		if (!barrel.depositItem(player, hand, stackInHand, slot)) {
			EnumDyeColor dyeColor = ColorHelper.getDyeColor(stackInHand);
			if (dyeColor != null) {
				barrel.applyDye(slot, stackInHand, dyeColor, false);
			}
		}
		return true;
	}

	/** Sneaking with a dye on the front colors every slot's count; vanilla doesn't activate the block for a sneaking player with an item. */
	public boolean tryToDyeAll(World world, BlockPos pos, IBlockState state, EnumFacing side, ItemStack stack) {
		if (side != getFacing(state)) {
			return false;
		}
		EnumDyeColor dyeColor = ColorHelper.getDyeColor(stack);
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		return dyeColor != null && be instanceof LimitedBarrelBlockEntity && ((LimitedBarrelBlockEntity) be).applyDye(0, stack, dyeColor, true);
	}

	/** The slot under the point on the front, as the front lays them out: one, two above each other, one over two, or two by two. */
	public int getInteractionSlot(IBlockState state, float hitX, float hitY, float hitZ) {
		if (numberOfInventorySlots == 1) {
			return 0;
		}
		Vec3d hit = new Vec3d(hitX - 0.5, hitY - 0.5, hitZ - 0.5);
		boolean top = dot(hit, getFrontUp(state)) > 0;
		boolean right = dot(hit, getFrontRight(state)) > 0;
		if (numberOfInventorySlots == 2) {
			return top ? 0 : 1;
		} else if (numberOfInventorySlots == 3) {
			if (top) {
				return 0;
			}
			return right ? 2 : 1;
		}
		if (top) {
			return right ? 1 : 0;
		}
		return right ? 3 : 2;
	}

	private static double dot(Vec3d vec, EnumFacing direction) {
		return vec.x * direction.getXOffset() + vec.y * direction.getYOffset() + vec.z * direction.getZOffset();
	}

	/** A left click on the front takes from the slot hit: a stack sneaking, else one. */
	public boolean tryToTakeItem(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!(be instanceof LimitedBarrelBlockEntity) || ((LimitedBarrelBlockEntity) be).isPacked()) {
			return false;
		}
		RayTraceResult hit = getFrontHit(world, pos, state, player);
		if (hit == null) {
			return false;
		}
		Vec3d hitVec = hit.hitVec;
		int slot = getInteractionSlot(state, (float) (hitVec.x - pos.getX()), (float) (hitVec.y - pos.getY()), (float) (hitVec.z - pos.getZ()));
		return ((LimitedBarrelBlockEntity) be).tryToTakeItem(player, slot);
	}

	/** Where the player looks on the barrel's front; null when it's not the front, or another block. */
	@Nullable
	public RayTraceResult getFrontHit(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
		Vec3d eyes = player.getPositionEyes(1);
		Vec3d end = eyes.add(player.getLook(1).scale(reach));
		RayTraceResult hit = world.rayTraceBlocks(eyes, end, false, false, true);
		if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || !pos.equals(hit.getBlockPos()) || hit.sideHit != getFacing(state)) {
			return null;
		}
		return hit;
	}

	/** In survival, the server's left click: on the front, it takes items out. */
	@Override
	public void onBlockClicked(World world, BlockPos pos, EntityPlayer player) {
		if (!world.isRemote) {
			tryToTakeItem(world, pos, world.getBlockState(pos), player);
		}
	}

	public boolean isLookingAtFront(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		return getFrontHit(world, pos, state, player) != null;
	}

	/** Hitting the front takes items out, it doesn't chip the barrel: no bits fly from it. */
	@SideOnly(Side.CLIENT)
	@Override
	public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
		if (target.sideHit == getFacing(state)) {
			return true;
		}
		return super.addHitEffects(state, world, target, manager);
	}
}
