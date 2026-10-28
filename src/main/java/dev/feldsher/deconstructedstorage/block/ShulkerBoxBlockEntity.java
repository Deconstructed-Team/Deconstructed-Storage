package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.Config;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

/**
 * A placed shulker box. Its lid rises and turns as it opens, as vanilla's does, on both sides: the box is as tall as its
 * lid stands, and pushes what's in the lid's way out of it. It holds no shulker boxes, and nothing the config forbids.
 */
public class ShulkerBoxBlockEntity extends StorageBlockEntity {
	public static final String STORAGE_TYPE = "shulker_box";

	public enum AnimationStatus {
		CLOSED, OPENING, OPENED, CLOSING
	}

	private AnimationStatus animationStatus = AnimationStatus.CLOSED;
	private float progress;
	private float progressOld;

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	@Override
	protected boolean isAllowedInStorage(ItemStack stack) {
		return isAllowedInShulkerBox(stack);
	}

	/** No shulker boxes, ours or vanilla, and nothing the config forbids: placed, or carried as an item. */
	public static boolean isAllowedInShulkerBox(ItemStack stack) {
		Block block = Block.getBlockFromItem(stack.getItem());
		return !(block instanceof ShulkerBoxBlock) && !(block instanceof BlockShulkerBox) && !Config.SERVER.isItemDisallowedInShulkerBox(stack.getItem());
	}

	@Override
	protected void onOpen() {
		playSound(SoundEvents.BLOCK_SHULKER_BOX_OPEN, getFacing());
	}

	@Override
	protected void onClose() {
		playSound(SoundEvents.BLOCK_SHULKER_BOX_CLOSE, getFacing());
	}

	/** Opening and closing follow the openers, whose count the clients get from the server. */
	@Override
	protected void animationTick() {
		if (isOpen() && (animationStatus == AnimationStatus.CLOSED || animationStatus == AnimationStatus.CLOSING)) {
			animationStatus = AnimationStatus.OPENING;
			notifyNeighbors();
		} else if (!isOpen() && (animationStatus == AnimationStatus.OPENED || animationStatus == AnimationStatus.OPENING)) {
			animationStatus = AnimationStatus.CLOSING;
			notifyNeighbors();
		}
		progressOld = progress;
		switch (animationStatus) {
			case CLOSED:
				progress = 0;
				break;
			case OPENING:
				progress += 0.1F;
				if (progress >= 1) {
					moveCollidedEntities();
					animationStatus = AnimationStatus.OPENED;
					progress = 1;
					notifyNeighbors();
				}
				break;
			case CLOSING:
				progress -= 0.1F;
				if (progress <= 0) {
					animationStatus = AnimationStatus.CLOSED;
					progress = 0;
					notifyNeighbors();
				}
				break;
			default:
				progress = 1;
				break;
		}
		if (animationStatus == AnimationStatus.OPENING) {
			moveCollidedEntities();
		}
	}

	private void notifyNeighbors() {
		if (world != null) {
			world.notifyNeighborsOfStateChange(pos, getBlockType(), false);
		}
	}

	private EnumFacing getFacing() {
		IBlockState state = getBlockState();
		return state.getBlock() instanceof ShulkerBoxBlock ? state.getValue(ShulkerBoxBlock.FACING) : EnumFacing.UP;
	}

	public AnimationStatus getAnimationStatus() {
		return animationStatus;
	}

	public float getProgress(float partialTicks) {
		return progressOld + (progress - progressOld) * partialTicks;
	}

	public boolean isClosed() {
		return animationStatus == AnimationStatus.CLOSED;
	}

	/** As vanilla's: a whole block, and the lid's rise on the side it faces. */
	public AxisAlignedBB getBoundingBox(EnumFacing facing) {
		return Block.FULL_BLOCK_AABB.expand(0.5F * getProgress(1) * facing.getXOffset(), 0.5F * getProgress(1) * facing.getYOffset(),
				0.5F * getProgress(1) * facing.getZOffset());
	}

	private AxisAlignedBB getTopBoundingBox(EnumFacing facing) {
		EnumFacing opposite = facing.getOpposite();
		return getBoundingBox(facing).contract(opposite.getXOffset(), opposite.getYOffset(), opposite.getZOffset());
	}

	/** As vanilla's: what the rising lid would go into is moved out of its way. */
	private void moveCollidedEntities() {
		if (world == null) {
			return;
		}
		EnumFacing facing = getFacing();
		AxisAlignedBB box = getTopBoundingBox(facing).offset(pos);
		List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(null, box);
		for (Entity entity : entities) {
			if (entity.getPushReaction() == EnumPushReaction.IGNORE) {
				continue;
			}
			AxisAlignedBB entityBox = entity.getEntityBoundingBox();
			double x = 0;
			double y = 0;
			double z = 0;
			switch (facing.getAxis()) {
				case X:
					x = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? box.maxX - entityBox.minX : entityBox.maxX - box.minX;
					x += 0.01D;
					break;
				case Y:
					y = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? box.maxY - entityBox.minY : entityBox.maxY - box.minY;
					y += 0.01D;
					break;
				default:
					z = facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? box.maxZ - entityBox.minZ : entityBox.maxZ - box.minZ;
					z += 0.01D;
					break;
			}
			entity.move(MoverType.SHULKER_BOX, x * facing.getXOffset(), y * facing.getYOffset(), z * facing.getZOffset());
		}
	}
}
