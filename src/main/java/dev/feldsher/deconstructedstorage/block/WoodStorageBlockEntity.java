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
	@Nullable
	private WoodType woodType = null;

	@Override
	protected void saveSynchronizedData(NBTTagCompound tag) {
		super.saveSynchronizedData(tag);
		if (woodType != null) {
			tag.setString(WOOD_TYPE_TAG, woodType.getName());
		}
	}

	@Override
	protected void loadSynchronizedData(NBTTagCompound tag) {
		super.loadSynchronizedData(tag);
		woodType = WoodType.fromName(tag.getString(WOOD_TYPE_TAG));
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
