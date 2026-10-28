package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedstorage.block.StorageBlockContents;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/** A dropped shulker box with what it holds: destroyed, as by lava or a cactus, it spills it, as vanilla's 1.21 box does. */
public class ShulkerBoxItemEntity extends EntityItem {
	@SuppressWarnings("unused") // as an entity's, for loading
	public ShulkerBoxItemEntity(World world) {
		super(world);
	}

	/** Takes the place of the item entity the game made: all of it, its motion and pickup delay too, but its UUID. */
	public ShulkerBoxItemEntity(World world, EntityItem original, ItemStack stack) {
		super(world, original.posX, original.posY, original.posZ, stack);
		NBTTagCompound tag = original.writeToNBT(new NBTTagCompound());
		tag.removeTag("UUIDMost");
		tag.removeTag("UUIDLeast");
		readFromNBT(tag);
		setItem(stack);
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		boolean wasDead = isDead;
		boolean damaged = super.attackEntityFrom(source, amount);
		if (!world.isRemote && isDead && !wasDead) {
			spillContents();
		}
		return damaged;
	}

	private void spillContents() {
		ItemStack stack = getItem();
		StorageBlockItem.getContentsUuid(stack).ifPresent(uuid -> {
			StackStorageWrapper wrapper = StackStorageWrapper.fromStack(stack);
			InventoryHelper.dropItems(wrapper.getInventoryHandler(), world, getPosition());
			InventoryHelper.dropItems(wrapper.getUpgradeHandler(), world, getPosition());
			StorageBlockContents.removeContents(uuid);
		});
	}
}
