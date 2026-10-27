package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Used on a storage, toggles what its mode says: the lock, or whether the lock, the tier or the upgrades show. Sneak
 * and scroll with it in hand changes the mode. In the offhand as a storage is placed, in the lock mode, it locks it.
 * Linking to a controller and the count and fill level displays come with the storages that have them.
 */
public class StorageToolItem extends Item {
	private static final String MODE_TAG = "mode";

	public enum Mode {
		LOCK(StorageBlockEntity::toggleLock, "toggling_lock"),
		LOCK_DISPLAY(StorageBlockEntity::toggleLockVisibility, "toggling_lock_display"),
		TIER_DISPLAY(StorageBlockEntity::toggleTierVisibility, "toggling_tier_display"),
		UPGRADES_DISPLAY(StorageBlockEntity::toggleUpgradesVisibility, "toggling_upgrades_display");

		private final Consumer<StorageBlockEntity> toggle;
		private final String overlayKey;

		Mode(Consumer<StorageBlockEntity> toggle, String overlayKey) {
			this.toggle = toggle;
			this.overlayKey = overlayKey;
		}

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		public Mode previous() {
			return values()[Math.floorMod(ordinal() - 1, values().length)];
		}
	}

	public StorageToolItem() {
		setMaxStackSize(1);
		setFull3D();
	}

	public static Mode getMode(ItemStack tool) {
		NBTTagCompound tag = tool.getTagCompound();
		if (tag != null) {
			for (Mode mode : Mode.values()) {
				if (mode.name().equals(tag.getString(MODE_TAG))) {
					return mode;
				}
			}
		}
		return Mode.LOCK;
	}

	public static void cycleMode(ItemStack tool, boolean next) {
		Mode mode = next ? getMode(tool).next() : getMode(tool).previous();
		StorageNbt.getOrCreateTag(tool).setString(MODE_TAG, mode.name());
	}

	/** What the overlay says the tool does in its mode. */
	public static TextComponentTranslation getOverlayMessage(ItemStack tool) {
		return new TextComponentTranslation(tool.getItem().getTranslationKey() + ".overlay." + getMode(tool).overlayKey);
	}

	/** In the offhand of who placed the storage: in the lock mode, the storage is placed locked. */
	public static void useOffHandOnPlaced(ItemStack tool, StorageBlockEntity be) {
		if (be.getWorld() != null && !be.getWorld().isRemote && getMode(tool) == Mode.LOCK) {
			be.toggleLock();
		}
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof StorageBlockEntity)) {
			return EnumActionResult.PASS;
		}
		if (!world.isRemote) {
			getMode(player.getHeldItem(hand)).toggle.accept((StorageBlockEntity) te);
		}
		return EnumActionResult.SUCCESS;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		for (String line : StorageTranslationHelper.INSTANCE.getTranslatedLines(getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX)) {
			tooltip.add(TextFormatting.DARK_GRAY + line);
		}
		String combination = TextFormatting.AQUA + new TextComponentTranslation(getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX + ".controls.combination")
				.getUnformattedText() + TextFormatting.GRAY;
		tooltip.add(TextFormatting.GRAY + new TextComponentTranslation(getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX + ".controls", combination)
				.getUnformattedText());
		tooltip.add(TextFormatting.WHITE + getOverlayMessage(stack).getUnformattedText());
		super.addInformation(stack, world, tooltip, flag);
	}
}
