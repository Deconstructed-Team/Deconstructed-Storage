package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedcore.util.StorageNbt;
import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.ControllerBlockEntity;
import dev.feldsher.deconstructedstorage.block.IMaterialHolder;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import dev.feldsher.deconstructedstorage.util.DecorationHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Decorates placed storages as the decoration table set it up: with the blocks of a barrel's materials, or with a main
 * and an accent color; used on a controller, all of its storages. It takes the blocks or dyes from the player's
 * inventory, and the backpacks and such in it, keeping what's left of each for the next time.
 */
public class PaintbrushItem extends Item {
	private static final String MATERIALS_TAG = "barrel_materials";
	private static final String MAIN_COLOR_TAG = "main_color";
	private static final String ACCENT_COLOR_TAG = "accent_color";
	private static final String REMAINING_PARTS_TAG = "remaining_parts";

	public PaintbrushItem() {
		setMaxStackSize(1);
		setFull3D();
	}

	public static void setBarrelMaterials(ItemStack paintbrush, Map<BarrelMaterial, MaterialBlock> materials) {
		StorageNbt.getOrCreateTag(paintbrush).setTag(MATERIALS_TAG, BarrelBlockItem.writeMaterials(materials));
		removeTag(paintbrush, MAIN_COLOR_TAG);
		removeTag(paintbrush, ACCENT_COLOR_TAG);
	}

	public static Map<BarrelMaterial, MaterialBlock> getBarrelMaterials(ItemStack paintbrush) {
		NBTTagCompound tag = paintbrush.getTagCompound();
		return tag == null ? Collections.emptyMap() : BarrelBlockItem.readMaterials(tag.getCompoundTag(MATERIALS_TAG));
	}

	private static boolean hasBarrelMaterials(ItemStack paintbrush) {
		return !getBarrelMaterials(paintbrush).isEmpty();
	}

	public static void setMainColor(ItemStack paintbrush, int mainColor) {
		StorageNbt.getOrCreateTag(paintbrush).setInteger(MAIN_COLOR_TAG, mainColor);
		removeTag(paintbrush, MATERIALS_TAG);
	}

	public static void setAccentColor(ItemStack paintbrush, int accentColor) {
		StorageNbt.getOrCreateTag(paintbrush).setInteger(ACCENT_COLOR_TAG, accentColor);
		removeTag(paintbrush, MATERIALS_TAG);
	}

	public static int getMainColor(ItemStack paintbrush) {
		NBTTagCompound tag = paintbrush.getTagCompound();
		return tag != null && tag.hasKey(MAIN_COLOR_TAG) ? tag.getInteger(MAIN_COLOR_TAG) : -1;
	}

	public static int getAccentColor(ItemStack paintbrush) {
		NBTTagCompound tag = paintbrush.getTagCompound();
		return tag != null && tag.hasKey(ACCENT_COLOR_TAG) ? tag.getInteger(ACCENT_COLOR_TAG) : -1;
	}

	private static void removeTag(ItemStack stack, String key) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			tag.removeTag(key);
		}
	}

	public static Map<String, Integer> getRemainingParts(ItemStack paintbrush) {
		NBTTagCompound tag = paintbrush.getTagCompound();
		Map<String, Integer> remainingParts = new HashMap<>();
		if (tag != null) {
			NBTTagCompound partsTag = tag.getCompoundTag(REMAINING_PARTS_TAG);
			for (String key : partsTag.getKeySet()) {
				remainingParts.put(key, partsTag.getInteger(key));
			}
		}
		return remainingParts;
	}

	public static void setRemainingParts(ItemStack paintbrush, Map<String, Integer> remainingParts) {
		NBTTagCompound partsTag = new NBTTagCompound();
		remainingParts.forEach(partsTag::setInteger);
		StorageNbt.getOrCreateTag(paintbrush).setTag(REMAINING_PARTS_TAG, partsTag);
	}

	/** What decorating the storage there, or a controller's storages, takes of what the player has, and what's missing. */
	public static Optional<ItemRequirements> getItemRequirements(ItemStack paintbrush, EntityPlayer player, World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (te == null) {
			return Optional.empty();
		}
		List<StorageBlockEntity> storages = getStorages(te);
		Map<String, Integer> allPartsNeeded = new HashMap<>();
		Map<BarrelMaterial, MaterialBlock> materialsToApply = getBarrelMaterials(paintbrush);
		for (StorageBlockEntity storage : storages) {
			Map<String, Integer> partsNeeded;
			if (!materialsToApply.isEmpty()) {
				if (!(storage instanceof IMaterialHolder)) {
					continue;
				}
				partsNeeded = getMaterialHolderPartsNeeded(materialsToApply, (IMaterialHolder) storage);
			} else {
				StorageWrapper wrapper = storage.getMainStorageWrapper();
				partsNeeded = DecorationHelper.getDyePartsNeeded(getMainColor(paintbrush), getAccentColor(paintbrush), wrapper.getMainColor(), wrapper.getAccentColor());
			}
			partsNeeded.forEach((part, count) -> allPartsNeeded.merge(part, count, Integer::sum));
		}
		if (allPartsNeeded.isEmpty()) {
			return Optional.empty();
		}
		Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
		Map<String, Integer> remainingAfter = new HashMap<>(remainingParts);
		DecorationHelper.ConsumptionResult result = DecorationHelper.consumePartsNeeded(allPartsNeeded, InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player),
				PaintbrushItem::partMatches, remainingAfter, true);
		List<ItemStack> itemsPresent = new ArrayList<>();
		List<ItemStack> itemsMissing = new ArrayList<>();
		allPartsNeeded.forEach((part, partsNeeded) -> getPartItem(part).ifPresent(item -> {
			int count = ceilDiv(partsNeeded - remainingParts.getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int missing = ceilDiv(result.getMissingParts().getOrDefault(part, 0), DecorationHelper.BLOCK_TOTAL_PARTS);
			int present = count - missing;
			if (missing > 0) {
				itemsMissing.add(copyWithCount(item, missing));
			}
			if (present > 0) {
				itemsPresent.add(copyWithCount(item, present));
			}
		}));
		return Optional.of(new ItemRequirements(itemsPresent, itemsMissing));
	}

	private static int ceilDiv(int value, int divisor) {
		return value <= 0 ? 0 : (value + divisor - 1) / divisor;
	}

	private static ItemStack copyWithCount(ItemStack stack, int count) {
		ItemStack copy = stack.copy();
		copy.setCount(count);
		return copy;
	}

	/** The item a part key stands for: the dye of the color, or the material's block. */
	@SuppressWarnings("deprecation")
	private static Optional<ItemStack> getPartItem(String part) {
		switch (part) {
			case DecorationHelper.RED_DYE:
				return Optional.of(new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()));
			case DecorationHelper.GREEN_DYE:
				return Optional.of(new ItemStack(Items.DYE, 1, EnumDyeColor.GREEN.getDyeDamage()));
			case DecorationHelper.BLUE_DYE:
				return Optional.of(new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()));
			default:
				MaterialBlock block = MaterialBlock.parse(part);
				return block == null ? Optional.empty() : block.getState().map(state -> new ItemStack(state.getBlock(), 1, state.getBlock().damageDropped(state)));
		}
	}

	private static boolean partMatches(String part, ItemStack stack) {
		if (part.startsWith("dye")) {
			return DecorationHelper.isDye(stack, part);
		}
		return MaterialBlock.of(stack).map(block -> block.toString().equals(part)).orElse(false);
	}

	public static Map<String, Integer> getMaterialHolderPartsNeeded(Map<BarrelMaterial, MaterialBlock> materialsToApply, IMaterialHolder materialHolder) {
		Map<BarrelMaterial, MaterialBlock> originalMaterials = new EnumMap<>(BarrelMaterial.class);
		originalMaterials.putAll(materialHolder.getMaterials());
		BarrelBlockItem.uncompactMaterials(originalMaterials);
		return DecorationHelper.getMaterialPartsNeeded(originalMaterials, materialsToApply);
	}

	/** The storage, or a controller's storages; a double chest by its main half. */
	private static List<StorageBlockEntity> getStorages(TileEntity te) {
		if (te instanceof StorageBlockEntity) {
			return Collections.singletonList(((StorageBlockEntity) te).getMainBlockEntity());
		}
		if (te instanceof ControllerBlockEntity) {
			return ((ControllerBlockEntity) te).getStorages(StorageBlockEntity.class, be -> true);
		}
		return Collections.emptyList();
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		ItemStack paintbrush = player.getHeldItem(hand);
		if (getMainColor(paintbrush) == -1 && getAccentColor(paintbrush) == -1 && !hasBarrelMaterials(paintbrush)) {
			return EnumActionResult.PASS;
		}
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof StorageBlockEntity) && !(te instanceof ControllerBlockEntity)) {
			return EnumActionResult.PASS;
		}
		if (!world.isRemote) {
			float soundVolume = te instanceof ControllerBlockEntity ? 0.6F : 1;
			for (StorageBlockEntity storage : getStorages(te)) {
				paintStorage(player, paintbrush, storage, soundVolume);
			}
		}
		return EnumActionResult.SUCCESS;
	}

	private static void paintStorage(EntityPlayer player, ItemStack paintbrush, StorageBlockEntity storage, float soundVolume) {
		StorageWrapper wrapper = storage.getMainStorageWrapper();
		IMaterialHolder materialHolder = storage instanceof IMaterialHolder ? (IMaterialHolder) storage : null;
		boolean painted = hasBarrelMaterials(paintbrush) ? materialHolder != null && materialHolder.canHoldMaterials() && applyMaterials(player, paintbrush, materialHolder, wrapper)
				: setColors(player, paintbrush, wrapper, materialHolder);
		if (painted) {
			IBlockState state = storage.getBlockState();
			EnumFacing effectSide = state.getBlock() instanceof StorageBlockBase ? ((StorageBlockBase) state.getBlock()).getFacing(state) : EnumFacing.UP;
			playSoundAndParticles(storage.getWorld(), storage.getPos(), soundVolume, state, effectSide);
		}
	}

	private static boolean setColors(EntityPlayer player, ItemStack paintbrush, StorageWrapper wrapper, @Nullable IMaterialHolder materialHolder) {
		Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
		List<IItemHandler> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		int mainColorToSet = getMainColor(paintbrush);
		int accentColorToSet = getAccentColor(paintbrush);
		int originalMainColor = wrapper.getMainColor();
		int originalAccentColor = wrapper.getAccentColor();
		if (originalMainColor == mainColorToSet && originalAccentColor == accentColorToSet
				|| !DecorationHelper.consumeDyes(mainColorToSet, accentColorToSet, remainingParts, itemHandlers, originalMainColor, originalAccentColor, true)) {
			return false;
		}
		wrapper.setColors(mainColorToSet, accentColorToSet);
		if (materialHolder != null) {
			materialHolder.setMaterials(Collections.emptyMap());
		}
		DecorationHelper.consumeDyes(mainColorToSet, accentColorToSet, remainingParts, itemHandlers, originalMainColor, originalAccentColor, false);
		setRemainingParts(paintbrush, remainingParts);
		return true;
	}

	private static boolean applyMaterials(EntityPlayer player, ItemStack paintbrush, IMaterialHolder materialHolder, StorageWrapper wrapper) {
		List<IItemHandler> itemHandlers = InventoryHelper.getItemHandlersFromPlayerIncludingContainers(player);
		Map<String, Integer> remainingParts = getRemainingParts(paintbrush);
		Map<BarrelMaterial, MaterialBlock> originalMaterials = new EnumMap<>(BarrelMaterial.class);
		originalMaterials.putAll(materialHolder.getMaterials());
		Map<BarrelMaterial, MaterialBlock> materialsToApply = new EnumMap<>(BarrelMaterial.class);
		materialsToApply.putAll(getBarrelMaterials(paintbrush));
		if (originalMaterials.equals(materialsToApply)) {
			return false;
		}
		BarrelBlockItem.uncompactMaterials(originalMaterials);
		if (!DecorationHelper.consumeMaterials(remainingParts, itemHandlers, originalMaterials, materialsToApply, true)) {
			return false;
		}
		DecorationHelper.consumeMaterials(remainingParts, itemHandlers, originalMaterials, materialsToApply, false);
		setRemainingParts(paintbrush, remainingParts);
		wrapper.setColors(-1, -1);
		BarrelBlockItem.compactMaterials(materialsToApply);
		materialHolder.setMaterials(materialsToApply);
		return true;
	}

	/** The block's place sound, and happy villager sparkles on its front. */
	private static void playSoundAndParticles(@Nullable World world, BlockPos pos, float soundVolume, IBlockState state, EnumFacing side) {
		if (world == null) {
			return;
		}
		world.playSound(null, pos, state.getBlock().getSoundType(state, world, pos, null).getPlaceSound(), SoundCategory.BLOCKS, soundVolume, 1);
		if (world instanceof WorldServer) {
			double x = pos.getX() + 0.5D + side.getXOffset() * 0.6D;
			double y = pos.getY() + 0.5D + side.getYOffset() * 0.6D;
			double z = pos.getZ() + 0.5D + side.getZOffset() * 0.6D;
			boolean vertical = side.getAxis() == EnumFacing.Axis.Y;
			double xOffset = vertical ? 0.4D : 0.1D + side.getZOffset() * 0.3D;
			double yOffset = vertical ? 0.1D : 0.4D;
			double zOffset = vertical ? 0.4D : 0.1D + side.getXOffset() * 0.3D;
			((WorldServer) world).spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 4, xOffset, yOffset, zOffset, 1);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		String tooltipKey = getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX;
		for (String line : StorageTranslationHelper.INSTANCE.getTranslatedLines(tooltipKey)) {
			tooltip.add(TextFormatting.DARK_GRAY + line);
		}
		Map<BarrelMaterial, MaterialBlock> materials = getBarrelMaterials(stack);
		if (!materials.isEmpty()) {
			tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".materials"));
			materials.forEach((material, block) -> getPartItem(block.toString()).ifPresent(blockStack ->
					tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".material", I18n.format(StorageTranslationHelper.INSTANCE.translGui("barrel_part." + material.getSerializedName())),
							TextFormatting.DARK_AQUA + blockStack.getDisplayName() + TextFormatting.GRAY))));
		}
		int mainColor = getMainColor(stack);
		if (mainColor != -1) {
			tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".main_color", String.format("#%06X", mainColor & 0xFFFFFF)));
		}
		int accentColor = getAccentColor(stack);
		if (accentColor != -1) {
			tooltip.add(TextFormatting.GRAY + I18n.format(tooltipKey + ".accent_color", String.format("#%06X", accentColor & 0xFFFFFF)));
		}
	}

	public static final class ItemRequirements {
		private final List<ItemStack> itemsPresent;
		private final List<ItemStack> itemsMissing;

		public ItemRequirements(List<ItemStack> itemsPresent, List<ItemStack> itemsMissing) {
			this.itemsPresent = itemsPresent;
			this.itemsMissing = itemsMissing;
		}

		public List<ItemStack> getItemsPresent() {
			return itemsPresent;
		}

		public List<ItemStack> getItemsMissing() {
			return itemsMissing;
		}
	}
}
