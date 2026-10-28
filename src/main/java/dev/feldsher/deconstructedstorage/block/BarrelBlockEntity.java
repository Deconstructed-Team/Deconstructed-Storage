package dev.feldsher.deconstructedstorage.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/** A placed barrel: open while someone has it open, which its top shows. */
public class BarrelBlockEntity extends WoodStorageBlockEntity {
	public static final String STORAGE_TYPE = "barrel";
	// 1.12 has no barrels, nor their sounds: Future MC's when it's there, a chest's when not
	private static final ResourceLocation FUTURE_MC_BARREL_OPEN = new ResourceLocation("futuremc", "barrel_open");
	private static final ResourceLocation FUTURE_MC_BARREL_CLOSE = new ResourceLocation("futuremc", "barrel_close");

	@Override
	protected String getStorageType() {
		return STORAGE_TYPE;
	}

	@Override
	protected int getNumberOfDisplayItems() {
		return 4;
	}

	@Override
	protected void onOpen() {
		playSound(getSound(FUTURE_MC_BARREL_OPEN, SoundEvents.BLOCK_CHEST_OPEN), getFacing());
	}

	@Override
	protected void onClose() {
		playSound(getSound(FUTURE_MC_BARREL_CLOSE, SoundEvents.BLOCK_CHEST_CLOSE), getFacing());
	}

	private static SoundEvent getSound(ResourceLocation name, SoundEvent fallback) {
		SoundEvent sound = SoundEvent.REGISTRY.getObject(name);
		return sound != null ? sound : fallback;
	}

	private EnumFacing getFacing() {
		if (world == null) {
			return EnumFacing.NORTH;
		}
		IBlockState state = world.getBlockState(pos);
		return state.getBlock() instanceof StorageBlockBase ? ((StorageBlockBase) state.getBlock()).getFacing(state) : EnumFacing.NORTH;
	}
}
