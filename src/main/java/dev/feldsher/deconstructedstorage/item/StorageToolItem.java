package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.controller.ILinkable;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.ControllerBlockEntity;
import dev.feldsher.deconstructedstorage.block.ICountDisplay;
import dev.feldsher.deconstructedstorage.block.IFillLevelDisplay;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Used on a storage, toggles what its mode says: the lock, or whether the lock, the counts, the tier, the upgrades or
 * the fill levels show; on a controller, that for all of its storages. In the link mode, a click on a controller
 * remembers it, and one on a storage or a link links it to that controller, or unlinks it when none is remembered;
 * sneaking, a click into the air forgets it. Sneak and scroll with it in hand changes the mode. In the offhand as a
 * storage is placed, it links or locks it so.
 */
public class StorageToolItem extends Item {
	private static final String MODE_TAG = "mode";
	private static final String CONTROLLER_POS_TAG = "controllerPos";

	public enum Mode {
		LINK(be -> {}, be -> false, be -> false, "linking"),
		LOCK(StorageBlockEntity::toggleLock, StorageBlockEntity::isLocked, be -> true, "toggling_lock"),
		COUNT_DISPLAY(be -> ((ICountDisplay) be).toggleCountVisibility(), be -> ((ICountDisplay) be).shouldShowCounts(), ICountDisplay.class::isInstance,
				"toggling_count_display"),
		LOCK_DISPLAY(StorageBlockEntity::toggleLockVisibility, StorageBlockEntity::shouldShowLock, StorageBlockEntity::isLocked, "toggling_lock_display"),
		TIER_DISPLAY(StorageBlockEntity::toggleTierVisibility, StorageBlockEntity::shouldShowTier, be -> true, "toggling_tier_display"),
		UPGRADES_DISPLAY(StorageBlockEntity::toggleUpgradesVisibility, StorageBlockEntity::shouldShowUpgrades, be -> true, "toggling_upgrades_display"),
		FILL_LEVEL_DISPLAY(be -> ((IFillLevelDisplay) be).toggleFillLevelVisibility(), be -> ((IFillLevelDisplay) be).shouldShowFillLevels(),
				IFillLevelDisplay.class::isInstance, "toggling_fill_level_display");

		private final Consumer<StorageBlockEntity> toggle;
		private final Predicate<StorageBlockEntity> isOn;
		private final Predicate<StorageBlockEntity> appliesTo;
		private final String overlayKey;

		Mode(Consumer<StorageBlockEntity> toggle, Predicate<StorageBlockEntity> isOn, Predicate<StorageBlockEntity> appliesTo, String overlayKey) {
			this.toggle = toggle;
			this.isOn = isOn;
			this.appliesTo = appliesTo;
			this.overlayKey = overlayKey;
		}

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		public Mode previous() {
			return values()[Math.floorMod(ordinal() - 1, values().length)];
		}

		/**
		 * On the storages of a controller: those that have it off get it on; when none has, all get it off. A storage
		 * that it doesn't apply to, as a lock display to an unlocked one, is left be.
		 */
		public void toggle(List<StorageBlockEntity> storages) {
			storages.removeIf(be -> !appliesTo.test(be));
			boolean anyOff = storages.stream().anyMatch(be -> !isOn.test(be));
			storages.stream().filter(be -> !anyOff || !isOn.test(be)).forEach(toggle);
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
		return Mode.LINK;
	}

	public static void cycleMode(ItemStack tool, boolean next) {
		Mode mode = next ? getMode(tool).next() : getMode(tool).previous();
		StorageNbt.getOrCreateTag(tool).setString(MODE_TAG, mode.name());
	}

	public static Optional<BlockPos> getControllerLink(ItemStack tool) {
		NBTTagCompound tag = tool.getTagCompound();
		return tag != null && tag.hasKey(CONTROLLER_POS_TAG) ? Optional.of(BlockPos.fromLong(tag.getLong(CONTROLLER_POS_TAG))) : Optional.empty();
	}

	private static void setControllerLink(ItemStack tool, BlockPos pos) {
		StorageNbt.getOrCreateTag(tool).setLong(CONTROLLER_POS_TAG, pos.toLong());
	}

	private static void removeControllerLink(ItemStack tool) {
		NBTTagCompound tag = tool.getTagCompound();
		if (tag != null) {
			tag.removeTag(CONTROLLER_POS_TAG);
		}
	}

	/** What the overlay says the tool does in its mode; linking, to which controller. */
	public static TextComponentTranslation getOverlayMessage(ItemStack tool) {
		String prefix = tool.getItem().getTranslationKey() + ".overlay.";
		if (getMode(tool) == Mode.LINK) {
			return getControllerLink(tool).map(pos -> new TextComponentTranslation(prefix + "linking", pos.getX(), pos.getY(), pos.getZ()))
					.orElseGet(() -> new TextComponentTranslation(prefix + "unlinking"));
		}
		return new TextComponentTranslation(prefix + getMode(tool).overlayKey);
	}

	/** In the offhand of who placed the storage: linked to the remembered controller, or locked. */
	public static void useOffHandOnPlaced(ItemStack tool, StorageBlockEntity be) {
		if (be.getWorld() == null || be.getWorld().isRemote) {
			return;
		}
		Mode mode = getMode(tool);
		if (mode == Mode.LINK) {
			Optional<BlockPos> controllerLink = getControllerLink(tool);
			if (controllerLink.isPresent()) {
				be.linkToController(controllerLink.get());
			} else {
				be.unlinkFromController();
			}
		} else if (mode == Mode.LOCK) {
			be.toggleLock();
		}
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		TileEntity te = world.getTileEntity(pos);
		ItemStack tool = player.getHeldItem(hand);
		Mode mode = getMode(tool);
		if (mode == Mode.LINK) {
			if (te instanceof ControllerBlockEntity) {
				setControllerLink(tool, pos);
				return EnumActionResult.SUCCESS;
			}
			if (te instanceof ILinkable) {
				if (!world.isRemote) {
					Optional<BlockPos> controllerLink = getControllerLink(tool);
					if (controllerLink.isPresent()) {
						((ILinkable) te).linkToController(controllerLink.get());
					} else {
						((ILinkable) te).unlinkFromController();
					}
				}
				return EnumActionResult.SUCCESS;
			}
			return EnumActionResult.PASS;
		}
		if (te instanceof ControllerBlockEntity) {
			if (!world.isRemote) {
				mode.toggle(((ControllerBlockEntity) te).getStorages(StorageBlockEntity.class, be -> true));
			}
			return EnumActionResult.SUCCESS;
		}
		if (!(te instanceof StorageBlockEntity)) {
			return EnumActionResult.PASS;
		}
		if (!world.isRemote && mode.appliesTo.test((StorageBlockEntity) te)) {
			mode.toggle.accept((StorageBlockEntity) te);
		}
		return EnumActionResult.SUCCESS;
	}

	/** Sneaking, a click into the air forgets the remembered controller. */
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack tool = player.getHeldItem(hand);
		if (player.isSneaking() && getControllerLink(tool).isPresent()) {
			removeControllerLink(tool);
			if (!world.isRemote) {
				player.sendStatusMessage(getOverlayMessage(tool), true);
			}
			return new ActionResult<>(EnumActionResult.SUCCESS, tool);
		}
		return super.onItemRightClick(world, player, hand);
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
