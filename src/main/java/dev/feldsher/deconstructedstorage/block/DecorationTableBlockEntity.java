package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedstorage.item.BarrelBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.util.DecorationHelper;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A decoration table: a storage, or what else it decorates, in its slot comes out made of the blocks in its seven
 * material slots, one for each part of a barrel, or tinted by the colors picked, which take dyes. A material slot left
 * empty takes the block of the one it inherits from, as the side the top's, unless that's switched off. What's left of
 * a block or dye after a decoration is kept for the next.
 */
public class DecorationTableBlockEntity extends TileEntity {
	public static final int TOP_INNER_TRIM_SLOT = 0;
	public static final int TOP_TRIM_SLOT = 1;
	public static final int SIDE_TRIM_SLOT = 2;
	public static final int BOTTOM_TRIM_SLOT = 3;
	public static final int TOP_CORE_SLOT = 4;
	public static final int SIDE_CORE_SLOT = 5;
	public static final int BOTTOM_CORE_SLOT = 6;
	public static final int RED_DYE_SLOT = 0;
	public static final int GREEN_DYE_SLOT = 1;
	public static final int BLUE_DYE_SLOT = 2;

	private static final Map<Predicate<ItemStack>, IItemDecorator> ITEM_DECORATORS = new LinkedHashMap<>();

	public static void registerItemDecorator(Predicate<ItemStack> itemMatcher, IItemDecorator itemDecorator) {
		ITEM_DECORATORS.put(itemMatcher, itemDecorator);
	}

	private final Map<String, Integer> remainingParts = new HashMap<>();

	private final ItemStackHandler decorativeBlocks = new ItemStackHandler(7) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndMarkDirty();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return isDecorativeBlock(stack);
		}
	};

	private final ItemStackHandler dyes = new ItemStackHandler(3) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndMarkDirty();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			switch (slot) {
				case RED_DYE_SLOT:
					return DecorationHelper.isDye(stack, DecorationHelper.RED_DYE);
				case GREEN_DYE_SLOT:
					return DecorationHelper.isDye(stack, DecorationHelper.GREEN_DYE);
				case BLUE_DYE_SLOT:
					return DecorationHelper.isDye(stack, DecorationHelper.BLUE_DYE);
				default:
					return false;
			}
		}
	};

	private final ItemStackHandler storageBlock = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			updateResultAndMarkDirty();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return getItemDecorator(stack).isPresent();
		}
	};

	private ItemStack result = ItemStack.EMPTY;
	private final Map<Integer, Boolean> slotMaterialInheritance = new HashMap<>();
	private int accentColor = -1;
	private int mainColor = -1;
	private final Set<String> missingDyes = new HashSet<>();

	/** A full block that isn't a storage: what a barrel can be made of. */
	@SuppressWarnings("deprecation")
	public static boolean isDecorativeBlock(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemBlock) || stack.getItem() instanceof StorageBlockItem) {
			return false;
		}
		Block block = ((ItemBlock) stack.getItem()).getBlock();
		try {
			return block.getStateFromMeta(stack.getItem().getMetadata(stack.getMetadata())).isFullCube();
		} catch (RuntimeException e) {
			return false;
		}
	}

	public void updateResultAndMarkDirty() {
		updateResult();
		markDirty();
	}

	private void updateResult() {
		missingDyes.clear();
		result = ItemStack.EMPTY;
		ItemStack input = storageBlock.getStackInSlot(0);
		if (input.isEmpty()) {
			return;
		}
		getItemDecorator(input).ifPresent(itemDecorator -> {
			TintDecorationResult decorationResult = decorateItem(itemDecorator, input);
			result = decorationResult.getResult();
			missingDyes.addAll(calculateMissingDyes(decorationResult.getRequiredDyeParts()));
		});
	}

	private static Optional<IItemDecorator> getItemDecorator(ItemStack input) {
		if (input.isEmpty()) {
			return Optional.empty();
		}
		return ITEM_DECORATORS.entrySet().stream().filter(e -> e.getKey().test(input)).findFirst().map(Map.Entry::getValue);
	}

	private TintDecorationResult decorateItem(IItemDecorator itemDecorator, ItemStack input) {
		if (itemDecorator.supportsMaterials(input)) {
			Map<BarrelMaterial, MaterialBlock> materialsToApply = getMaterialsToApply(itemDecorator.supportsTopInnerTrim(input));
			if (!materialsToApply.isEmpty()) {
				return new TintDecorationResult(itemDecorator.decorateWithMaterials(input, materialsToApply), Collections.emptyMap());
			}
		}
		if (itemDecorator.supportsTints(input)) {
			return itemDecorator.decorateWithTints(input, mainColor, accentColor);
		}
		return TintDecorationResult.EMPTY;
	}

	public boolean hasMaterials() {
		return !InventoryHelper.isEmpty(decorativeBlocks);
	}

	/** What the screen shows turning: the decorated storage, or for a decorated paintbrush what it would paint. */
	public List<ItemStack> getDecoratedPreviewStacks() {
		ItemStack input = storageBlock.getStackInSlot(0);
		return getItemDecorator(input).map(itemDecorator -> {
			List<ItemStack> previewStacks = new ArrayList<>();
			for (ItemStack stack : itemDecorator.getPreviewStackInputs(input, hasMaterials())) {
				getItemDecorator(stack).ifPresent(inputItemDecorator -> {
					ItemStack decorated = decorateItem(inputItemDecorator, stack).getResult();
					if (!decorated.isEmpty()) {
						previewStacks.add(decorated);
					}
				});
			}
			return previewStacks;
		}).orElse(Collections.emptyList());
	}

	public static boolean allMaterialsMatch(Map<BarrelMaterial, MaterialBlock> newMaterials, Map<BarrelMaterial, MaterialBlock> currentMaterials) {
		return newMaterials.equals(currentMaterials);
	}

	/** The dyes a color needs that have no slot filled: with all three dyes in, none is missing yet. */
	private Set<String> calculateMissingDyes(Map<String, Integer> requiredDyeParts) {
		Set<String> missing = new HashSet<>();
		if (!dyes.getStackInSlot(RED_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(GREEN_DYE_SLOT).isEmpty() && !dyes.getStackInSlot(BLUE_DYE_SLOT).isEmpty()) {
			return missing;
		}
		for (String dye : requiredDyeParts.keySet()) {
			if (dye.equals(DecorationHelper.RED_DYE) && dyes.getStackInSlot(RED_DYE_SLOT).isEmpty()
					|| dye.equals(DecorationHelper.GREEN_DYE) && dyes.getStackInSlot(GREEN_DYE_SLOT).isEmpty()
					|| dye.equals(DecorationHelper.BLUE_DYE) && dyes.getStackInSlot(BLUE_DYE_SLOT).isEmpty()) {
				missing.add(dye);
			}
		}
		return missing;
	}

	public Set<String> getMissingDyes() {
		return missingDyes;
	}

	private Map<BarrelMaterial, MaterialBlock> getMaterialsToApply(boolean supportsInnerTrim) {
		Map<BarrelMaterial, MaterialBlock> materials = new EnumMap<>(BarrelMaterial.class);
		MaterialBlock topInnerTrim = setMaterialFromBlock(TOP_INNER_TRIM_SLOT, null, materials, BarrelMaterial.TOP_INNER_TRIM, supportsInnerTrim);
		MaterialBlock topTrim = setMaterialFromBlock(TOP_TRIM_SLOT, topInnerTrim, materials, BarrelMaterial.TOP_TRIM, true);
		MaterialBlock sideTrim = setMaterialFromBlock(SIDE_TRIM_SLOT, topTrim, materials, BarrelMaterial.SIDE_TRIM, true);
		setMaterialFromBlock(BOTTOM_TRIM_SLOT, sideTrim, materials, BarrelMaterial.BOTTOM_TRIM, true);
		MaterialBlock top = setMaterialFromBlock(TOP_CORE_SLOT, topTrim, materials, BarrelMaterial.TOP, true);
		MaterialBlock side = setMaterialFromBlock(SIDE_CORE_SLOT, top, materials, BarrelMaterial.SIDE, true);
		setMaterialFromBlock(BOTTOM_CORE_SLOT, side, materials, BarrelMaterial.BOTTOM, true);
		return materials;
	}

	@Nullable
	private MaterialBlock setMaterialFromBlock(int slotIndex, @Nullable MaterialBlock defaultMaterial, Map<BarrelMaterial, MaterialBlock> materials, BarrelMaterial material,
			boolean addToMaterials) {
		MaterialBlock materialBlock = MaterialBlock.of(decorativeBlocks.getStackInSlot(slotIndex)).orElse(isSlotMaterialInherited(slotIndex) ? defaultMaterial : null);
		if (materialBlock != null && addToMaterials) {
			materials.put(material, materialBlock);
		}
		return materialBlock;
	}

	public ItemStackHandler getDecorativeBlocks() {
		return decorativeBlocks;
	}

	public ItemStackHandler getDyes() {
		return dyes;
	}

	public ItemStackHandler getStorageBlock() {
		return storageBlock;
	}

	public ItemStack getResult() {
		return result;
	}

	public ItemStack extractResult(int count) {
		if (result.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack extracted = result.copy();
		extracted.setCount(Math.min(count, result.getCount()));
		if (count >= result.getCount()) {
			result = ItemStack.EMPTY;
		} else {
			result.shrink(count);
		}
		markDirty();
		return extracted;
	}

	public boolean isSlotMaterialInherited(int slot) {
		return slotMaterialInheritance.getOrDefault(slot, true);
	}

	/** The block an empty slot takes from the slots it inherits from, the nearest that has one. */
	public ItemStack getInheritedItem(int childSlot) {
		while (isSlotMaterialInherited(childSlot)) {
			int parentSlot = getSlotInheritedFrom(childSlot);
			if (parentSlot == -1) {
				return ItemStack.EMPTY;
			} else if (!decorativeBlocks.getStackInSlot(parentSlot).isEmpty()) {
				return decorativeBlocks.getStackInSlot(parentSlot);
			}
			childSlot = parentSlot;
		}
		return ItemStack.EMPTY;
	}

	public static int getSlotInheritedFrom(int slot) {
		switch (slot) {
			case TOP_TRIM_SLOT:
				return TOP_INNER_TRIM_SLOT;
			case SIDE_TRIM_SLOT:
			case TOP_CORE_SLOT:
				return TOP_TRIM_SLOT;
			case BOTTOM_TRIM_SLOT:
				return SIDE_TRIM_SLOT;
			case SIDE_CORE_SLOT:
				return TOP_CORE_SLOT;
			case BOTTOM_CORE_SLOT:
				return SIDE_CORE_SLOT;
			default:
				return -1;
		}
	}

	public void setSlotMaterialInheritance(int slot, boolean value) {
		if (value) {
			slotMaterialInheritance.remove(slot);
		} else {
			slotMaterialInheritance.put(slot, false);
		}
		updateResultAndMarkDirty();
	}

	/** The decoration made: the blocks or dyes it takes go, what's left of them kept. */
	public void consumeIngredientsOnCraft() {
		ItemStack input = storageBlock.getStackInSlot(0);
		getItemDecorator(input).ifPresent(itemDecorator -> {
			if (!itemDecorator.consumesIngredientsOnCraft()) {
				return;
			}
			boolean emptyDecorativeBlocks = InventoryHelper.isEmpty(decorativeBlocks);
			if ((emptyDecorativeBlocks || !itemDecorator.supportsMaterials(input)) && itemDecorator.supportsTints(input)) {
				DecorationHelper.consumeDyes(mainColor, accentColor, remainingParts, Collections.singletonList(dyes), StorageBlockItem.getMainColor(input).orElse(-1),
						StorageBlockItem.getAccentColor(input).orElse(-1), false);
			} else if (!emptyDecorativeBlocks && itemDecorator.supportsMaterials(input)) {
				Map<BarrelMaterial, MaterialBlock> originalMaterials = new EnumMap<>(BarrelMaterial.class);
				originalMaterials.putAll(BarrelBlockItem.getMaterials(input));
				BarrelBlockItem.uncompactMaterials(originalMaterials);
				DecorationHelper.consumeMaterials(remainingParts, Collections.singletonList(decorativeBlocks), originalMaterials,
						getMaterialsToApply(itemDecorator.supportsTopInnerTrim(input)), false);
			}
			markDirty();
			notifyBlockUpdate();
		});
	}

	/** What the result takes, of each block or dye. */
	public Map<String, Integer> getPartsNeeded() {
		Map<String, Integer> partsNeeded = new HashMap<>();
		ItemStack storageStack = storageBlock.getStackInSlot(0);
		if (InventoryHelper.isEmpty(decorativeBlocks) || !(storageStack.getItem() instanceof BarrelBlockItem)) {
			partsNeeded.putAll(DecorationHelper.getDyePartsNeeded(mainColor, accentColor, StorageBlockItem.getMainColor(storageStack).orElse(-1),
					StorageBlockItem.getAccentColor(storageStack).orElse(-1)));
		} else {
			Map<BarrelMaterial, MaterialBlock> originalMaterials = new EnumMap<>(BarrelMaterial.class);
			originalMaterials.putAll(BarrelBlockItem.getMaterials(storageStack));
			BarrelBlockItem.uncompactMaterials(originalMaterials);
			partsNeeded.putAll(DecorationHelper.getMaterialPartsNeeded(originalMaterials, getMaterialsToApply(STORAGE_DECORATOR.supportsTopInnerTrim(storageStack))));
		}
		return partsNeeded;
	}

	public int getMainColor() {
		return mainColor;
	}

	public void setMainColor(int mainColor) {
		this.mainColor = mainColor;
		updateResultAndMarkDirty();
	}

	public int getAccentColor() {
		return accentColor;
	}

	public void setAccentColor(int accentColor) {
		this.accentColor = accentColor;
		updateResultAndMarkDirty();
	}

	public Map<String, Integer> getPartsStored() {
		return remainingParts;
	}

	public void dropContents() {
		if (world != null) {
			InventoryHelper.dropItems(decorativeBlocks, world, pos);
			InventoryHelper.dropItems(dyes, world, pos);
			InventoryHelper.dropItems(storageBlock, world, pos);
		}
	}

	private void notifyBlockUpdate() {
		if (world != null && !world.isRemote) {
			IBlockState state = world.getBlockState(pos);
			world.notifyBlockUpdate(pos, state, state, 3);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		loadData(tag);
	}

	private void loadData(NBTTagCompound tag) {
		decorativeBlocks.deserializeNBT(tag.getCompoundTag("decorativeBlocks"));
		dyes.deserializeNBT(tag.getCompoundTag("dyes"));
		storageBlock.deserializeNBT(tag.getCompoundTag("storageBlock"));
		result = tag.hasKey("result") ? new ItemStack(tag.getCompoundTag("result")) : ItemStack.EMPTY;
		slotMaterialInheritance.clear();
		NBTTagList inheritance = tag.getTagList("slotMaterialInheritance", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < inheritance.tagCount(); i++) {
			NBTTagCompound slotTag = inheritance.getCompoundTagAt(i);
			slotMaterialInheritance.put(slotTag.getInteger("slot"), slotTag.getBoolean("value"));
		}
		remainingParts.clear();
		NBTTagCompound remainingPartsTag = tag.getCompoundTag("remainingParts");
		for (String key : remainingPartsTag.getKeySet()) {
			remainingParts.put(key, remainingPartsTag.getInteger(key));
		}
		mainColor = tag.hasKey("mainColor") ? tag.getInteger("mainColor") : -1;
		accentColor = tag.hasKey("accentColor") ? tag.getInteger("accentColor") : -1;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		return saveData(tag);
	}

	private NBTTagCompound saveData(NBTTagCompound tag) {
		tag.setTag("decorativeBlocks", decorativeBlocks.serializeNBT());
		tag.setTag("dyes", dyes.serializeNBT());
		tag.setTag("storageBlock", storageBlock.serializeNBT());
		if (!result.isEmpty()) {
			tag.setTag("result", result.writeToNBT(new NBTTagCompound()));
		}
		NBTTagList inheritance = new NBTTagList();
		slotMaterialInheritance.forEach((slot, value) -> {
			NBTTagCompound slotTag = new NBTTagCompound();
			slotTag.setInteger("slot", slot);
			slotTag.setBoolean("value", value);
			inheritance.appendTag(slotTag);
		});
		tag.setTag("slotMaterialInheritance", inheritance);
		NBTTagCompound remainingPartsTag = new NBTTagCompound();
		remainingParts.forEach(remainingPartsTag::setInteger);
		tag.setTag("remainingParts", remainingPartsTag);
		tag.setInteger("mainColor", mainColor);
		tag.setInteger("accentColor", accentColor);
		return tag;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		return new SPacketUpdateTileEntity(pos, 0, saveData(new NBTTagCompound()));
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		loadData(pkt.getNbtCompound());
	}

	public static final class TintDecorationResult {
		public static final TintDecorationResult EMPTY = new TintDecorationResult(ItemStack.EMPTY, Collections.emptyMap());
		private final ItemStack result;
		private final Map<String, Integer> requiredDyeParts;

		public TintDecorationResult(ItemStack result, Map<String, Integer> requiredDyeParts) {
			this.result = result;
			this.requiredDyeParts = requiredDyeParts;
		}

		public ItemStack getResult() {
			return result;
		}

		public Map<String, Integer> getRequiredDyeParts() {
			return requiredDyeParts;
		}
	}

	/** What the table does to a kind of item put in its storage slot. */
	public interface IItemDecorator {
		default boolean consumesIngredientsOnCraft() {
			return true;
		}

		boolean supportsMaterials(ItemStack input);

		boolean supportsTints(ItemStack input);

		boolean supportsTopInnerTrim(ItemStack input);

		ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, MaterialBlock> materialsToApply);

		TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet);

		default List<ItemStack> getPreviewStackInputs(ItemStack input, boolean materialsInCraftingSlots) {
			return Collections.singletonList(input);
		}
	}

	/** Whether making this takes blocks or dyes: a paintbrush set up only remembers what it paints with. */
	public static boolean consumesIngredients(ItemStack result) {
		return getItemDecorator(result).map(IItemDecorator::consumesIngredientsOnCraft).orElse(true);
	}

	private static boolean isTintedStorage(ItemStack storage) {
		return StorageBlockItem.getMainColor(storage).isPresent() || StorageBlockItem.getAccentColor(storage).isPresent();
	}

	private static boolean colorsTransparentOrSameAs(ItemStack storage, int mainColorToSet, int accentColorToSet) {
		return (mainColorToSet == -1 || mainColorToSet == StorageBlockItem.getMainColor(storage).orElse(-1))
				&& (accentColorToSet == -1 || accentColorToSet == StorageBlockItem.getAccentColor(storage).orElse(-1));
	}

	/** The barrels without a top inner trim: those with one slot, plain or limited. */
	private static boolean hasTopInnerTrim(Item item) {
		Block block = Block.getBlockFromItem(item);
		if (block instanceof LimitedBarrelBlock) {
			return ((LimitedBarrelBlock) block).getNumberOfInventorySlots() > 1;
		}
		return !(block instanceof BarrelBlock);
	}

	public static final IItemDecorator STORAGE_DECORATOR = new IItemDecorator() {
		@Override
		public boolean supportsMaterials(ItemStack input) {
			return input.getItem() instanceof BarrelBlockItem && !isTintedStorage(input);
		}

		@Override
		public boolean supportsTints(ItemStack input) {
			return !(input.getItem() instanceof BarrelBlockItem) || BarrelBlockItem.getMaterials(input).isEmpty();
		}

		@Override
		public boolean supportsTopInnerTrim(ItemStack input) {
			return hasTopInnerTrim(input.getItem());
		}

		@Override
		public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, MaterialBlock> materialsToApply) {
			if (materialsToApply.isEmpty()) {
				return ItemStack.EMPTY;
			}
			Map<BarrelMaterial, MaterialBlock> materials = new EnumMap<>(BarrelMaterial.class);
			materials.putAll(BarrelBlockItem.getMaterials(input));
			BarrelBlockItem.uncompactMaterials(materials);
			materials.putAll(materialsToApply);
			BarrelBlockItem.compactMaterials(materials);
			if (allMaterialsMatch(materials, BarrelBlockItem.getMaterials(input))) {
				return ItemStack.EMPTY;
			}
			ItemStack result = input.copy();
			result.setCount(1);
			BarrelBlockItem.removeCoveredTints(result, materials);
			BarrelBlockItem.setMaterials(result, materials);
			return result;
		}

		@Override
		public TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
			if (colorsTransparentOrSameAs(input, mainColorToSet, accentColorToSet)) {
				return TintDecorationResult.EMPTY;
			}
			ItemStack result = input.copy();
			result.setCount(1);
			if (mainColorToSet != -1) {
				StorageBlockItem.setMainColor(result, mainColorToSet);
			}
			if (accentColorToSet != -1) {
				StorageBlockItem.setAccentColor(result, accentColorToSet);
			}
			return new TintDecorationResult(result, DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, StorageBlockItem.getMainColor(input).orElse(-1),
					StorageBlockItem.getAccentColor(input).orElse(-1)));
		}
	};

	/** Leather armor takes the main color, from 24 parts of dyes. */
	private static final IItemDecorator DYEABLE_DECORATOR = new IItemDecorator() {
		@Override
		public boolean supportsMaterials(ItemStack input) {
			return false;
		}

		@Override
		public boolean supportsTints(ItemStack input) {
			return true;
		}

		@Override
		public boolean supportsTopInnerTrim(ItemStack input) {
			return false;
		}

		@Override
		public ItemStack decorateWithMaterials(ItemStack input, Map<BarrelMaterial, MaterialBlock> materialsToApply) {
			return ItemStack.EMPTY;
		}

		@Override
		public TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
			ItemArmor armor = (ItemArmor) input.getItem();
			int currentColor = armor.hasColor(input) ? armor.getColor(input) : -1;
			if (mainColorToSet == -1 || currentColor == mainColorToSet) {
				return TintDecorationResult.EMPTY;
			}
			ItemStack result = input.copy();
			result.setCount(1);
			armor.setColor(result, mainColorToSet & 0xFFFFFF);
			return new TintDecorationResult(result, DecorationHelper.getDyePartsNeeded(mainColorToSet, -1, currentColor, -1, 24, 0));
		}
	};

	static {
		registerItemDecorator(stack -> stack.getItem() instanceof StorageBlockItem, STORAGE_DECORATOR);
		registerItemDecorator(stack -> stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER,
				DYEABLE_DECORATOR);
	}
}
