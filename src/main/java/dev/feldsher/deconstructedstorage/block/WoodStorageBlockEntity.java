package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A storage made of wood, which it keeps the kind of. One with both colors set is tinted, and of no wood; one with
 * neither is acacia, as 1.21 makes them.
 */
public abstract class WoodStorageBlockEntity extends StorageBlockEntity {
	private static final String WOOD_TYPE_TAG = "woodType";
	private static final String PACKED_TAG = "packed";
	@Nullable
	private WoodType woodType = null;
	private boolean packed = false;

	@Override
	protected void saveSynchronizedData(NBTTagCompound tag) {
		super.saveSynchronizedData(tag);
		if (woodType != null) {
			tag.setString(WOOD_TYPE_TAG, woodType.getName());
		}
		if (packed) {
			tag.setBoolean(PACKED_TAG, true);
		}
	}

	@Override
	protected void loadSynchronizedData(NBTTagCompound tag) {
		super.loadSynchronizedData(tag);
		woodType = WoodType.fromName(tag.getString(WOOD_TYPE_TAG));
		packed = tag.getBoolean(PACKED_TAG);
	}

	/**
	 * Packed with tape, it's shut: it opens to nobody, no pipe reaches in, its upgrades rest; broken, it goes as a stack
	 * with all it holds.
	 */
	public boolean isPacked() {
		return packed;
	}

	/** Packed, it leaves its controller's network: nothing reaches in. */
	public void setPacked(boolean packed) {
		if (packed && !this.packed && world != null && !world.isRemote) {
			removeFromController();
		}
		this.packed = packed;
		refreshTickableUpgrades();
		markDirty();
		notifyBlockUpdate();
	}

	@Override
	public boolean canBeConnected() {
		return !packed && super.canBeConnected();
	}

	@Override
	protected boolean canTickUpgrades() {
		return !packed;
	}

	@Override
	public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
		return !(packed && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) && super.hasCapability(capability, facing);
	}

	@Nullable
	@Override
	public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.util.EnumFacing facing) {
		return packed && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? null : super.getCapability(capability, facing);
	}

	public Optional<WoodType> getWoodType() {
		if (woodType == null && !(getStorageWrapper().hasMainColor() && getStorageWrapper().hasAccentColor())) {
			return Optional.of(WoodType.DEFAULT);
		}
		return Optional.ofNullable(woodType);
	}

	/** Named after its wood, as its stack is: "Spruce Barrel". */
	@Override
	public String getStorageDisplayName() {
		if (hasCustomName()) {
			return super.getStorageDisplayName();
		}
		return WoodStorageBlockItem.getDisplayName(getBlockType().getTranslationKey() + ".name", getWoodType().orElse(null));
	}

	public void setWoodType(@Nullable WoodType woodType) {
		this.woodType = woodType;
		markDirty();
		notifyBlockUpdate();
	}
}
