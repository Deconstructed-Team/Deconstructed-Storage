package dev.feldsher.deconstructedstorage.upgrades.hopper;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.upgrades.ContentsFilterLogic;
import dev.feldsher.deconstructedcore.upgrades.FilterLogic;
import dev.feldsher.deconstructedcore.upgrades.ITickableUpgrade;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.common.gui.BlockSide;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pulls items in from the inventories on its pull sides and pushes them out to those on its push sides, a stack of up
 * to its size every so many ticks. Where there's no inventory block, it works on an entity there that has one, as a
 * minecart with a chest. 1.12 keeps no capability caches: the inventories are looked up each time it moves items.
 */
public class HopperUpgradeWrapper extends UpgradeWrapperBase<HopperUpgradeWrapper, HopperUpgradeItem> implements ITickableUpgrade {
	private static final String PULL_DIRECTIONS_TAG = "pullDirections";
	private static final String PUSH_DIRECTIONS_TAG = "pushDirections";

	private final Set<EnumFacing> pullDirections = new LinkedHashSet<>();
	private final Set<EnumFacing> pushDirections = new LinkedHashSet<>();

	private final ContentsFilterLogic inputFilterLogic;
	private final TargetContentsFilterLogic outputFilterLogic;
	private long coolDownTime = 0;

	protected HopperUpgradeWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, java.util.function.Consumer<ItemStack> upgradeSaveHandler) {
		super(storageWrapper, upgrade, upgradeSaveHandler);
		inputFilterLogic = new ContentsFilterLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getInputFilterSlotCount(), storageWrapper::getInventoryHandler, "input_filter_attributes");
		outputFilterLogic = new TargetContentsFilterLogic(upgrade, this.upgradeSaveHandler, upgradeItem.getOutputFilterSlotCount(), storageWrapper::getInventoryHandler, "output_filter_attributes");

		deserialize();
	}

	@Override
	public void tick(@Nullable Entity entity, World world, BlockPos pos) {
		initDirections(world, pos);

		if (coolDownTime > world.getTotalWorldTime()) {
			return;
		}

		for (EnumFacing pushDirection : pushDirections) {
			if (runOnItemHandlers(world, pos, pushDirection, this::pushItems, entity)) {
				break;
			}
		}

		for (EnumFacing pullDirection : pullDirections) {
			if (runOnItemHandlers(world, pos, pullDirection, this::pullItems, entity)) {
				break;
			}
		}

		coolDownTime = world.getTotalWorldTime() + upgradeItem.getTransferSpeedTicks();
	}

	/** Until set in its tab, it pulls from above the storage and pushes below, as the storage is turned. */
	private void initDirections(World world, BlockPos pos) {
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null && (tag.hasKey(PUSH_DIRECTIONS_TAG) || tag.hasKey(PULL_DIRECTIONS_TAG))) {
			return;
		}
		IBlockState state = world.getBlockState(pos);
		pullDirections.clear();
		pushDirections.clear();
		if (state.getBlock() instanceof StorageBlockBase) {
			StorageBlockBase storageBlock = (StorageBlockBase) state.getBlock();
			EnumFacing horizontalDirection = storageBlock.getHorizontalDirection(state);
			initDirections(BlockSide.BOTTOM.toDirection(horizontalDirection, storageBlock.getVerticalFacing(state)),
					BlockSide.TOP.toDirection(horizontalDirection, storageBlock.getVerticalFacing(state)));
		} else {
			initDirections(EnumFacing.DOWN, EnumFacing.UP);
		}
	}

	private boolean pullItems(List<IItemHandler> fromHandlers) {
		for (IItemHandler fromHandler : fromHandlers) {
			if (moveItems(fromHandler, storageWrapper.getInventoryForUpgradeProcessing(), inputFilterLogic)) {
				return true;
			}
		}
		return false;
	}

	private boolean pushItems(List<IItemHandler> toHandlers) {
		for (IItemHandler toHandler : toHandlers) {
			outputFilterLogic.setInventory(toHandler);
			if (moveItems(storageWrapper.getInventoryForUpgradeProcessing(), toHandler, outputFilterLogic)) {
				return true;
			}
		}
		return false;
	}

	private boolean moveItems(IItemHandler fromHandler, IItemHandler toHandler, FilterLogic filterLogic) {
		for (int slot = 0; slot < fromHandler.getSlots(); slot++) {
			ItemStack slotStack = fromHandler.getStackInSlot(slot);
			if (!slotStack.isEmpty() && filterLogic.matchesFilter(slotStack)) {
				ItemStack extractedStack = fromHandler.extractItem(slot, upgradeItem.getMaxTransferStackSize(), true);
				if (!extractedStack.isEmpty()) {
					ItemStack remainder = InventoryHelper.insertIntoInventory(extractedStack, toHandler, true);
					if (remainder.getCount() < extractedStack.getCount()) {
						InventoryHelper.insertIntoInventory(fromHandler.extractItem(slot, extractedStack.getCount() - remainder.getCount(), false), toHandler, false);
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean runOnItemHandlers(World world, BlockPos pos, EnumFacing direction, Predicate<List<IItemHandler>> run, @Nullable Entity entity) {
		List<BlockPos> offsetPositions = getOffsetPositions(world, pos, direction, entity);
		List<IItemHandler> handlers = new ArrayList<>();
		for (BlockPos offsetPos : offsetPositions) {
			if (!world.isBlockLoaded(offsetPos)) {
				continue;
			}
			TileEntity te = world.getTileEntity(offsetPos);
			if (te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())) {
				IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
				if (handler != null) {
					handlers.add(handler);
				}
			}
		}
		return handlers.isEmpty() ? runOnEntityItemHandlers(world, offsetPositions, direction, run, entity) : run.test(handlers);
	}

	private List<BlockPos> getOffsetPositions(World world, BlockPos pos, EnumFacing direction, @Nullable Entity entity) {
		IBlockState storageState = world.getBlockState(pos);
		return entity == null && storageState.getBlock() instanceof StorageBlockBase
				? ((StorageBlockBase) storageState.getBlock()).getNeighborPos(storageState, pos, direction) : Collections.singletonList(pos.offset(direction));
	}

	/** Not of living things: in 1.12 their equipment is an item handler too, which isn't to be taken from. */
	private boolean runOnEntityItemHandlers(World world, List<BlockPos> offsetPositions, EnumFacing direction, Predicate<List<IItemHandler>> run, @Nullable Entity entity) {
		List<Entity> entities = new ArrayList<>();
		for (BlockPos offsetPosition : offsetPositions) {
			entities.addAll(world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(offsetPosition),
					e -> e != null && e != entity && e.isEntityAlive() && !(e instanceof EntityLivingBase)));
		}
		if (!entities.isEmpty()) {
			Collections.shuffle(entities);
			for (Entity e : entities) {
				IItemHandler entityCap = e.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite())
						? e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite()) : null;
				if (entityCap != null) {
					return run.test(Collections.singletonList(entityCap));
				}
			}
		}
		return false;
	}

	public ContentsFilterLogic getInputFilterLogic() {
		return inputFilterLogic;
	}

	public ContentsFilterLogic getOutputFilterLogic() {
		return outputFilterLogic;
	}

	public boolean isPullingFrom(EnumFacing direction) {
		return pullDirections.contains(direction);
	}

	public boolean isPushingTo(EnumFacing direction) {
		return pushDirections.contains(direction);
	}

	public void setPullingFrom(EnumFacing direction, boolean shouldPull) {
		if (shouldPull) {
			pullDirections.add(direction);
		} else {
			pullDirections.remove(direction);
		}
		serializeDirections(PULL_DIRECTIONS_TAG, pullDirections);
	}

	public void setPushingTo(EnumFacing direction, boolean isPushing) {
		if (isPushing) {
			pushDirections.add(direction);
		} else {
			pushDirections.remove(direction);
		}
		serializeDirections(PUSH_DIRECTIONS_TAG, pushDirections);
	}

	private void serializeDirections(String key, Set<EnumFacing> directions) {
		int[] indexes = new int[directions.size()];
		int i = 0;
		for (EnumFacing direction : directions) {
			indexes[i++] = direction.getIndex();
		}
		StorageNbt.getOrCreateTag(upgrade).setIntArray(key, indexes);
		save();
	}

	private void deserialize() {
		pullDirections.clear();
		pushDirections.clear();
		NBTTagCompound tag = upgrade.getTagCompound();
		if (tag != null) {
			for (int index : tag.getIntArray(PULL_DIRECTIONS_TAG)) {
				pullDirections.add(EnumFacing.byIndex(index));
			}
			for (int index : tag.getIntArray(PUSH_DIRECTIONS_TAG)) {
				pushDirections.add(EnumFacing.byIndex(index));
			}
		}
	}

	public void initDirections(EnumFacing pushDirection, EnumFacing pullDirection) {
		setPushingTo(pushDirection, true);
		setPullingFrom(pullDirection, true);
	}
}
