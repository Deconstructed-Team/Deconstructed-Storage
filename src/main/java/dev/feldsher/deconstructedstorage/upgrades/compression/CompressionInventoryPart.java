package dev.feldsher.deconstructedstorage.upgrades.compression;

import dev.feldsher.deconstructedcore.inventory.IInventoryPartHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.SlotRange;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A run of slots that holds one item and what it compresses to, each slot the next step, as nuggets, ingots and
 * blocks. Inside, the slots keep as much as possible compressed; outside, each slot shows all that could be taken out
 * of it as that slot's item, the slots before it counted in, and taking out decompresses as needed. The chain starts
 * from the last filled slot, or, while all are empty, from the remembered ones; slots past where the chain ends can't
 * be used.
 */
public class CompressionInventoryPart implements IInventoryPartHandler {
	public static final String NAME = "compression";
	public static final String EMPTY_COMPRESSION_SLOT = DeconstructedStorage.MOD_ID + ":item/empty_compression_slot";
	private final InventoryHandler parent;
	private final SlotRange slotRange;
	private final Supplier<MemorySettingsCategory> getMemorySettings;

	private Map<Integer, SlotDefinition> slotDefinitions = new HashMap<>();
	private final Map<Integer, ItemStack> calculatedStacks = new HashMap<>();

	public CompressionInventoryPart(InventoryHandler parent, SlotRange slotRange, Supplier<MemorySettingsCategory> getMemorySettings) {
		this.parent = parent;
		this.slotRange = slotRange;
		this.getMemorySettings = getMemorySettings;
	}

	@Override
	public void onInit() {
		calculateStacks(true);
	}

	private void calculateStacks(boolean initial) {
		clearCollections();
		Map<Integer, ItemStack> existingStacks = getExistingStacks();

		if (existingStacks.isEmpty()) {
			return;
		}

		int lastNonEmptySlot = getLastNonEmptySlot(existingStacks);
		setSlotDefinitions(getSlotDefinitions(existingStacks.get(lastNonEmptySlot), lastNonEmptySlot, existingStacks), initial);

		compactInternalSlots();
		updateCalculatedStacks();
	}

	private void setSlotDefinitions(Map<Integer, SlotDefinition> definitions, boolean initial) {
		slotDefinitions = definitions;
		if (initial) {
			parent.initFilterItems();
		} else {
			parent.onFilterItemsChanged();
			slotDefinitions.forEach((slot, definition) -> parent.triggerOnChangeListeners(slot));
		}
	}

	private Integer getLastNonEmptySlot(Map<Integer, ItemStack> existingStacks) {
		for (int slot = slotRange.firstSlot() + slotRange.numberOfSlots() - 1; slot >= slotRange.firstSlot(); slot--) {
			if (existingStacks.containsKey(slot)) {
				return slot;
			}
		}

		return -1;
	}

	private Map<Integer, SlotDefinition> getSlotDefinitions(ItemStack firstItem, int lastSlot, Map<Integer, ItemStack> existingStacks) {
		Map<Integer, SlotDefinition> ret = new HashMap<>();
		addPreviousItems(ret, lastSlot, firstItem);

		ItemStack prevItem = firstItem;
		for (int slot = lastSlot; slot >= slotRange.firstSlot(); slot--) {
			if (existingStacks.containsKey(slot) && !isSameItem(existingStacks.get(slot), prevItem)) {
				ret.clear(); //clearing any compressible definition added before as the compression should no longer compress if there are incompatible items present
				break;
			} else {
				Optional<RecipeHelper.CompactingShape> compressionShape = getCompressionShape(prevItem);
				if (compressionShape.isPresent()) {
					RecipeHelper.CompactingShape shape = compressionShape.get();
					ret.put(slot, new SlotDefinition(prevItem, getNumberOfIngredients(shape), true));
					prevItem = RecipeHelper.getCompactingResult(prevItem, shape).getResult();
				} else {
					ret.put(slot, new SlotDefinition(prevItem, 1, true));
					break;
				}
			}
		}

		updateSlotLimits(ret);
		updateInaccessibleAndCompressible(ret, existingStacks);

		return ret;
	}

	private static int getNumberOfIngredients(RecipeHelper.CompactingShape shape) {
		switch (shape) {
			case TWO_BY_TWO:
			case TWO_BY_TWO_UNCRAFTABLE:
				return 4;
			case THREE_BY_THREE:
			case THREE_BY_THREE_UNCRAFTABLE:
				return 9;
			default:
				return 1;
		}
	}

	private void updateSlotLimits(Map<Integer, SlotDefinition> definitions) {
		int totalLimit = 0;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			if (definitions.containsKey(slot) && definitions.get(slot).isAccessible()) {
				totalLimit = intMaxCappedAddition(parent.getBaseStackLimit(definitions.get(slot).item), intMaxCappedMultiply(definitions.get(slot).prevSlotMultiplier, totalLimit));

				definitions.get(slot).setSlotLimit(totalLimit);
			}
		}
	}

	private static int intMaxCappedAddition(int a, int b) {
		return Integer.MAX_VALUE - a < b ? Integer.MAX_VALUE : a + b;
	}

	private static int intMaxCappedMultiply(int a, int b) {
		return b != 0 && Integer.MAX_VALUE / b < a ? Integer.MAX_VALUE : a * b;
	}

	private void updateCalculatedStacks() {
		int totalCalculated = 0;
		boolean prevFull = false;
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			if (!slotDefinition.isAccessible()) {
				continue;
			}
			if (!slotDefinition.isCompressible()) {
				calculatedStacks.put(slot, parent.getSlotStack(slot).copy());
				continue;
			}
			int internalCount = parent.getSlotStack(slot).getCount();
			totalCalculated = Integer.MAX_VALUE / slotDefinition.prevSlotMultiplier() < totalCalculated ? Integer.MAX_VALUE : totalCalculated * slotDefinition.prevSlotMultiplier();
			totalCalculated = Integer.MAX_VALUE - internalCount < totalCalculated ? Integer.MAX_VALUE : totalCalculated + internalCount;

			ItemStack calculatedStack = copyWithCount(slotDefinition.item(), totalCalculated);

			int internalLimit = parent.getBaseStackLimit(calculatedStack);
			int maxStackSize = calculatedStack.getMaxStackSize();
			if (Integer.MAX_VALUE - totalCalculated < maxStackSize) {
				calculatedStack.setCount(Integer.MAX_VALUE - (prevFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize));
			}
			calculatedStacks.put(slot, calculatedStack);

			prevFull = internalLimit <= internalCount;
		}
	}

	private void compactInternalSlots() {
		Map<Integer, Integer> toUpdate = new HashMap<>();

		for (int slot = slotRange.firstSlot() + 1; slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			ItemStack slotStack = parent.getSlotStack(slot);
			int multiplier = getPrevSlotMultiplier(slot);
			if (slotStack.isEmpty() || multiplier < 2) {
				continue;
			}
			int prevSlot = slot - 1;
			ItemStack prevStack = parent.getSlotStack(prevSlot);
			int stackLimit = parent.getBaseStackLimit(prevStack);
			int prevStackCount = toUpdate.containsKey(prevSlot) ? toUpdate.get(prevSlot) : prevStack.getCount();
			int availableSpace = stackLimit - prevStackCount;
			int countToInsert = Math.min(availableSpace, slotStack.getCount() / multiplier);
			if (countToInsert > 0) {
				toUpdate.put(prevSlot, prevStackCount + countToInsert);
				toUpdate.put(slot, slotStack.getCount() - countToInsert * multiplier);
			}
		}

		updateInternalStacksWithCounts(toUpdate);
	}

	private void updateInaccessibleAndCompressible(Map<Integer, SlotDefinition> definitions, Map<Integer, ItemStack> existingStacks) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			definitions.computeIfAbsent(slot, s -> {
				if (existingStacks.containsKey(s)) {
					return new SlotDefinition(existingStacks.get(s), 1, true);
				}
				return SlotDefinition.inaccessible();
			});
			if (!definitions.get(slot).isAccessible()) {
				continue;
			}
			boolean uncompressibleFromNext = definitions.containsKey(slot - 1) && definitions.get(slot - 1).isAccessible() && definitions.get(slot).prevSlotMultiplier() > 1;
			boolean compressibleFromPrevious = definitions.containsKey(slot + 1) && definitions.get(slot + 1).isAccessible() && definitions.get(slot + 1).prevSlotMultiplier() > 1;
			definitions.get(slot).setCompressible(uncompressibleFromNext || compressibleFromPrevious);
		}
	}

	private void clearCollections() {
		slotDefinitions.clear();
		calculatedStacks.clear();
		parent.onFilterItemsChanged();
	}

	private Optional<RecipeHelper.CompactingShape> getCompressionShape(ItemStack stack) {
		Set<RecipeHelper.CompactingShape> compactingShapes = RecipeHelper.getItemCompactingShapes(stack);

		if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE)) {
			return Optional.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE);
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE)) {
			return Optional.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE);
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.THREE_BY_THREE)) {
			Item compressedItem = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.THREE_BY_THREE).getResult().getItem();
			return getDecompressionResultFromConfig(compressedItem).isPresent() ? Optional.of(RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE) : Optional.empty();
		} else if (compactingShapes.contains(RecipeHelper.CompactingShape.TWO_BY_TWO)) {
			Item compressedItem = RecipeHelper.getCompactingResult(stack, RecipeHelper.CompactingShape.TWO_BY_TWO).getResult().getItem();
			return getDecompressionResultFromConfig(compressedItem).isPresent() ? Optional.of(RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE) : Optional.empty();
		}
		return Optional.empty();
	}

	private void addPreviousItems(Map<Integer, SlotDefinition> slotDefinitions, int firstFilledSlot, ItemStack firstFilledItem) {
		ItemStack currentItem = firstFilledItem;
		for (int slot = firstFilledSlot + 1; slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			RecipeHelper.UncompactingResult uncompactingResult = RecipeHelper.getUncompactingResult(currentItem);
			if (uncompactingResult.getCompactUsingShape() == RecipeHelper.CompactingShape.NONE) {
				Optional<RecipeHelper.UncompactingResult> decompressionResult = getDecompressionResultFromConfig(currentItem.getItem());
				if (!decompressionResult.isPresent()) {
					break;
				}
				uncompactingResult = decompressionResult.get();
			}
			slotDefinitions.put(slot, new SlotDefinition(uncompactingResult.getResult(), uncompactingResult.getCompactUsingShape() == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE ? 4 : 9, true));
			currentItem = uncompactingResult.getResult();
		}
	}

	Optional<RecipeHelper.UncompactingResult> getDecompressionResultFromConfig(Item currentItem) {
		return Config.SERVER.compressionUpgrade.getDecompressionResult(currentItem);
	}

	private Map<Integer, ItemStack> getExistingStacks() {
		Map<Integer, ItemStack> existingStacks = new LinkedHashMap<>();
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			ItemStack slotStack = parent.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				existingStacks.put(slot, slotStack);
			}
		}

		if (existingStacks.isEmpty()) {
			MemorySettingsCategory memorySettings = getMemorySettings.get();
			for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
				int finalSlot = slot;
				memorySettings.getSlotFilterStack(slot, true).ifPresent(stack -> existingStacks.put(finalSlot, stack));
			}
		}

		return existingStacks;
	}

	@Override
	public int getSlotLimit(int slot) {
		return slotDefinitions.containsKey(slot) ? slotDefinitions.get(slot).slotLimit() : parent.getBaseSlotLimit();
	}

	@Override
	public int getStackLimit(int slot, ItemStack stack) {
		if (!slotDefinitions.containsKey(slot)) {
			return parent.getBaseStackLimit(stack);
		}

		return getStackLimit(slotDefinitions.get(slot));
	}

	private int getStackLimit(SlotDefinition slotDefinition) {
		if (!slotDefinition.isAccessible()) {
			return 0;
		}

		return slotDefinition.slotLimit();
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return extractItem(slot, amount, simulate, s -> s.isEmpty() ? 64 : s.getMaxStackSize());
	}

	private ItemStack extractItem(int slot, int amount, boolean simulate, ToIntFunction<ItemStack> getLimit) {
		if (!slotDefinitions.containsKey(slot) || !slotDefinitions.get(slot).isAccessible() || !calculatedStacks.containsKey(slot)) {
			return ItemStack.EMPTY;
		}
		int toExtract = Math.min(calculatedStacks.get(slot).getCount(), amount);

		if (toExtract > 0) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			ItemStack slotStack = parent.getSlotStack(slot);
			toExtract = Math.min(toExtract, getLimit.applyAsInt(slotStack));
			ItemStack result = slotDefinition.isCompressible() ? copyWithCount(slotDefinition.item(), toExtract) : copyWithCount(slotStack, toExtract);

			if (!simulate) {
				if (slotDefinition.isCompressible()) {
					extractFromCalculated(slot, toExtract);
					extractFromInternal(slot, toExtract);
				} else {
					slotStack.shrink(toExtract);
					calculatedStacks.put(slot, slotStack.copy());
					parent.setSlotStack(slot, slotStack);
				}
				removeDefinitionsIfEmpty(slot);
			}

			return result;
		}

		return ItemStack.EMPTY;
	}

	private void removeDefinitionsIfEmpty(int slotTriggeringChange) {
		for (int slot = slotRange.firstSlot(); slot < slotRange.firstSlot() + slotRange.numberOfSlots(); slot++) {
			if (!parent.getSlotStack(slot).isEmpty() || getMemorySettings.get().getSlotFilterStack(slot, false).isPresent()) {
				return;
			}
		}

		clearCollections();
		parent.triggerOnChangeListeners(slotTriggeringChange);
	}

	private void extractFromInternal(int slotToStartFrom, int amountToExtract) {
		Map<Integer, Integer> toUpdate = new HashMap<>();
		int decompressedAmountToInsert = 0;
		int totalMultiplier = 1;
		while (amountToExtract > 0) {
			ItemStack slotStack = parent.getSlotStack(slotToStartFrom);
			if (totalMultiplier == 1) {
				int toRemove = Math.min(amountToExtract, slotStack.getCount());
				toUpdate.put(slotToStartFrom, slotStack.getCount() - toRemove);
				amountToExtract -= toRemove;
			} else {
				int ceiledAmount = (int) Math.ceil((double) amountToExtract / totalMultiplier);
				int toRemove = Math.min(ceiledAmount, slotStack.getCount());
				toUpdate.put(slotToStartFrom, slotStack.getCount() - toRemove);

				int totalToRemove = toRemove * totalMultiplier;
				if (totalToRemove > amountToExtract) {
					decompressedAmountToInsert = totalToRemove - amountToExtract;
					break;
				}
				amountToExtract -= totalToRemove;
			}

			totalMultiplier *= getPrevSlotMultiplier(slotToStartFrom);
			slotToStartFrom--;
		}

		while (decompressedAmountToInsert > 0) {
			slotToStartFrom++;
			totalMultiplier /= getPrevSlotMultiplier(slotToStartFrom);

			int toInsert = decompressedAmountToInsert / totalMultiplier;
			if (toInsert > 0) {
				toUpdate.put(slotToStartFrom, toUpdate.getOrDefault(slotToStartFrom, 0) + toInsert);
				decompressedAmountToInsert -= toInsert * totalMultiplier;
			}
		}

		updateInternalStacksWithCounts(toUpdate);
	}

	private int getPrevSlotMultiplier(int slot) {
		return slotDefinitions.get(slot).prevSlotMultiplier;
	}

	private void updateInternalStacksWithCounts(Map<Integer, Integer> toUpdate) {
		toUpdate.forEach((s, count) -> {
			ItemStack slotStack = parent.getSlotStack(s);
			if (slotStack.getCount() != count) {
				if (count == 0) {
					parent.setSlotStack(s, ItemStack.EMPTY);
				} else if (slotStack.isEmpty()) {
					parent.setSlotStack(s, copyWithCount(slotDefinitions.get(s).item(), count));
				} else {
					slotStack.setCount(count);
					parent.setSlotStack(s, slotStack);
				}
			}
		});
	}

	private void extractFromCalculated(int slot, int extractCount) {
		extractFromCalculatedThisAndPreviousStacks(extractCount, slot);
		extractFromCalculatedThisAndStacksAfter(extractCount, slot + 1);
	}

	private void extractFromCalculatedThisAndPreviousStacks(int extractCount, int slotCalculated) {
		int countBeforeChange = -1;
		int multiplier = 1;
		while (extractCount != 0 && calculatedStacks.containsKey(slotCalculated)) {
			ItemStack calculatedStack = calculatedStacks.get(slotCalculated);

			if (countBeforeChange > 0 && countBeforeChange / multiplier > calculatedStack.getCount()) {
				extractCount = calculatedStack.getCount() - (countBeforeChange - extractCount * multiplier) / multiplier;
				if (extractCount <= 0) {
					break;
				}
			}

			countBeforeChange = calculatedStack.getCount();
			int toSet = getCountChangeLeavingSpaceBeforeMaxInt(countBeforeChange - extractCount, slotCalculated, calculatedStack);
			calculatedStack.setCount(toSet);

			calculatedStacks.put(slotCalculated, calculatedStack);

			multiplier = getPrevSlotMultiplier(slotCalculated);
			extractCount = countBeforeChange / multiplier - calculatedStack.getCount() / multiplier;

			slotCalculated--;
		}
	}

	private int getCountChangeLeavingSpaceBeforeMaxInt(int countCalculated, int slotCalculated, ItemStack calculatedStack) {
		int toSet = countCalculated;
		int prevSlot = slotCalculated - 1;
		SlotDefinition prevSlotDefinition = slotDefinitions.get(prevSlot);
		boolean hasPrevious = prevSlotDefinition != null && prevSlotDefinition.isAccessible();
		if (countCalculated > 0 && Integer.MAX_VALUE - countCalculated < calculatedStack.getMaxStackSize() && hasPrevious) {
			boolean prevSlotFull = getSlotLimit(prevSlot) == calculatedStacks.get(prevSlot).getCount();
			int buffer = prevSlotFull ? getStackLimit(slotCalculated, calculatedStack) - countCalculated : calculatedStack.getMaxStackSize();
			toSet = Integer.MAX_VALUE - buffer;
		}
		return toSet;
	}

	private void extractFromCalculatedThisAndStacksAfter(int extractCount, int slot) {
		while (slot < slotRange.firstSlot() + slotRange.numberOfSlots() && slotDefinitions.get(slot).isAccessible()) {
			ItemStack calculatedStack = calculatedStacks.get(slot);
			int multiplier = getPrevSlotMultiplier(slot);
			extractCount *= multiplier;
			int countSet = calculatedStack.getCount() - extractCount;
			countSet = getCountChangeLeavingSpaceBeforeMaxInt(countSet, slot, calculatedStack);
			calculatedStack.setCount(countSet);
			calculatedStacks.put(slot, calculatedStack);
			slot++;
		}
	}

	@Override
	public boolean sharesCapacityAcrossSlots() {
		return true;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate, SlotInserter insertSuper) {
		return insertItem(slot, stack, simulate);
	}

	private ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (canNotBeInserted(slot, stack)) {
			return stack;
		}

		Map<Integer, SlotDefinition> definitions = slotDefinitions;

		if (definitions.isEmpty()) {
			definitions = getSlotDefinitions(stack, slot, Collections.emptyMap());
		}

		int limit = getStackLimit(definitions.get(slot));

		int currentCalculatedCount = calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0;
		int inserted = Math.min(Math.max(parent.getBaseStackLimit(stack) - parent.getSlotStack(slot).getCount(), limit - currentCalculatedCount), stack.getCount());

		if (inserted <= 0) {
			return stack;
		}

		ItemStack result = copyWithCount(stack, stack.getCount() - inserted);

		if (simulate) {
			return result;
		}

		if (!slotDefinitions.containsKey(slot)) {
			setSlotDefinitions(definitions, false);
			compactInternalSlots();
			updateCalculatedStacks();
		}

		if (slotDefinitions.get(slot).isCompressible()) {
			insertIntoInternalAndCalculated(slot, inserted);
		} else {
			calculatedStacks.compute(slot, (s, st) -> {
				if (st == null || st.isEmpty()) {
					return copyWithCount(stack, inserted);
				}
				st.grow(inserted);
				return st;
			});
			ItemStack slotStack = parent.getSlotStack(slot);
			if (slotStack.isEmpty()) {
				parent.setSlotStack(slot, copyWithCount(stack, inserted));
			} else {
				slotStack.grow(inserted);
				parent.setSlotStack(slot, slotStack);
			}
		}

		return result;
	}

	private boolean canNotBeInserted(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return true;
		}

		if (!slotDefinitions.containsKey(slot)) {
			return false;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return !slotDefinition.isAccessible() || !isSameItem(slotDefinition.item(), stack);
	}

	private void insertIntoInternalAndCalculated(int slotToStartFrom, long amountToInsert) {
		Map<Integer, Integer> toUpdate = new LinkedHashMap<>();
		Map<Integer, Integer> calculatedAdditions = new LinkedHashMap<>();
		int totalMultiplier = 1;
		int slot = slotToStartFrom;

		long amountToSet = amountToInsert + parent.getSlotStack(slot).getCount();

		while (amountToSet / ((long) totalMultiplier * getPrevSlotMultiplier(slot)) > 0 && slotDefinitions.containsKey(slot - 1) && slotDefinitions.get(slot - 1).isAccessible()) {
			totalMultiplier *= getPrevSlotMultiplier(slot);
			slot--;
			amountToSet += (long) parent.getSlotStack(slot).getCount() * totalMultiplier;
		}

		long calculatedAddition = 0;
		while (slot <= slotToStartFrom) {
			calculatedAddition *= getPrevSlotMultiplier(slot);
			ItemStack slotStack = parent.getSlotStack(slot);
			ItemStack limitStack = slotStack.isEmpty() ? slotDefinitions.get(slot).item() : slotStack;
			int toSet = (int) Math.min(amountToSet / totalMultiplier, parent.getBaseStackLimit(limitStack));
			calculatedAddition += (toSet - slotStack.getCount());
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));
			if (toSet > 0) {
				toUpdate.put(slot, toSet);
				amountToSet -= (long) toSet * totalMultiplier;
			} else {
				toUpdate.put(slot, 0);
			}

			if (amountToSet != 0) {
				if (!slotDefinitions.containsKey(slot + 1)) {
					DeconstructedStorage.LOGGER.error("Compression inventory is in an invalid state. Slot {} is compressible, there's stack remaining to insert but slot {} is not defined.\nSlot Definitions\n{}", slot, slot + 1, slotDefinitions);
					break;
				}
				totalMultiplier /= getPrevSlotMultiplier(slot + 1);
			}
			slot++;
		}

		//finish calculation of calculated addition to the follow up slots even though they are not getting their internal stack changed
		while (slot < slotRange.firstSlot() + slotRange.numberOfSlots()) {
			if (!slotDefinitions.containsKey(slot)) {
				break;
			}

			calculatedAddition *= getPrevSlotMultiplier(slot);
			calculatedAdditions.put(slot, (int) Math.min(calculatedAddition, Integer.MAX_VALUE));

			slot++;
		}

		updateInternalStacksWithCounts(toUpdate);

		calculatedAdditions.forEach(this::addToCalculatedStack);
		toUpdate.keySet().forEach(parent::triggerOnChangeListeners);
	}

	private void addToCalculatedStack(int slot, int countToAdd) {
		if (!calculatedStacks.containsKey(slot) || calculatedStacks.get(slot).isEmpty()) {
			SlotDefinition slotDefinition = slotDefinitions.get(slot);
			calculatedStacks.put(slot, copyWithCount(slotDefinition.item(), countToAdd));
			return;
		}
		ItemStack currentCalculated = calculatedStacks.get(slot);

		int totalCalculated = Integer.MAX_VALUE - countToAdd < currentCalculated.getCount() ? Integer.MAX_VALUE : currentCalculated.getCount() + countToAdd;

		int previousSlot = slot - 1;
		if (totalCalculated != Integer.MAX_VALUE || !slotDefinitions.containsKey(previousSlot)) {
			currentCalculated.setCount(totalCalculated);
			return;
		}

		ItemStack previousInternalStack = parent.getSlotStack(previousSlot);
		boolean isPreviousFull = previousInternalStack.getCount() >= parent.getBaseStackLimit(previousInternalStack);

		int internalLimit = parent.getBaseStackLimit(currentCalculated);
		int internalCount = parent.getSlotStack(slot).getCount();

		int maxStackSize = previousInternalStack.getMaxStackSize();
		int spaceBeforeMaxInt = isPreviousFull ? Math.min(maxStackSize, internalLimit - internalCount) : maxStackSize;
		currentCalculated.setCount(Integer.MAX_VALUE - spaceBeforeMaxInt);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack, BiConsumer<Integer, ItemStack> setStackInSlotSuper) {
		int currentCount = calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot).getCount() : 0;
		if (currentCount < stack.getCount()) {
			insertItem(slot, copyWithCount(stack, stack.getCount() - currentCount), false);
		} else if (currentCount > stack.getCount()) {
			extractItem(slot, currentCount - stack.getCount(), false, s -> Integer.MAX_VALUE);
		}
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack, @Nullable EntityPlayer player, BiPredicate<Integer, ItemStack> isItemValidSuper) {
		if (!slotDefinitions.containsKey(slot)) {
			return true;
		}

		SlotDefinition slotDefinition = slotDefinitions.get(slot);
		return slotDefinition.isAccessible() && isSameItem(slotDefinition.item(), stack);
	}

	@Override
	public ItemStack getStackInSlot(int slot, IntFunction<ItemStack> getStackInSlotSuper) {
		return slotDefinitions.containsKey(slot) && slotDefinitions.get(slot).isAccessible() && calculatedStacks.containsKey(slot) ? calculatedStacks.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public boolean isSlotAccessible(int slot) {
		return !slotDefinitions.containsKey(slot) || slotDefinitions.get(slot).isAccessible();
	}

	@Override
	public int getSlots() {
		return slotRange.numberOfSlots();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Nullable
	@Override
	public String getNoItemIcon(int slot) {
		return EMPTY_COMPRESSION_SLOT;
	}

	@Override
	public Item getFilterItem(int slot) {
		return slotDefinitions.containsKey(slot) ? slotDefinitions.get(slot).item().getItem() : Items.AIR;
	}

	@Override
	public void onSlotLimitChange() {
		updateSlotLimits(slotDefinitions);
	}

	@Override
	public Set<Integer> getNoSortSlots() {
		return IntStream.rangeClosed(slotRange.firstSlot(), slotRange.firstSlot() + slotRange.numberOfSlots() - 1).boxed().collect(Collectors.toSet());
	}

	@Override
	public void onSlotFilterChanged(int slot) {
		calculateStacks(false);
	}

	@Override
	public boolean isFilterItem(Item item) {
		for (SlotDefinition slotDefinition : slotDefinitions.values()) {
			if (slotDefinition.item().getItem() == item) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<Item, Set<Integer>> getFilterItems() {
		Map<Item, Set<Integer>> filterItems = new HashMap<>();
		for (Map.Entry<Integer, SlotDefinition> entry : slotDefinitions.entrySet()) {
			SlotDefinition slotDefinition = entry.getValue();
			if (slotDefinition.isAccessible()) {
				filterItems.computeIfAbsent(slotDefinition.item().getItem(), k -> new HashSet<>()).add(entry.getKey());
			}
		}
		return filterItems;
	}

	/** Same item, metadata and NBT: what 1.21 compares as the same item with the same components. */
	private static boolean isSameItem(ItemStack a, ItemStack b) {
		return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata() && ItemStack.areItemStackTagsEqual(a, b);
	}

	/** A copy with that count, kept as that item even at 0, as 1.21's copyWithCount: counts grow back from it. */
	private static ItemStack copyWithCount(ItemStack stack, int count) {
		ItemStack copy = new ItemStack(stack.getItem(), count, stack.getMetadata());
		if (stack.getTagCompound() != null) {
			copy.setTagCompound(stack.getTagCompound().copy());
		}
		return copy;
	}

	private static final class SlotDefinition {
		private final ItemStack item;
		private final int prevSlotMultiplier;
		private int slotLimit;
		private final boolean isAccessible;

		private boolean isCompressible = false;

		private SlotDefinition(ItemStack item, int prevSlotMultiplier, int slotLimit, boolean isAccessible) {
			this.item = item.isEmpty() ? ItemStack.EMPTY : copyWithCount(item, 1);
			this.prevSlotMultiplier = prevSlotMultiplier;
			this.slotLimit = slotLimit;
			this.isAccessible = isAccessible;
		}

		public static SlotDefinition inaccessible() {
			return new SlotDefinition(ItemStack.EMPTY, 0, 0, false);
		}

		public SlotDefinition(ItemStack stack, int prevSlotMultiplier, boolean isAccessible) {
			this(stack, prevSlotMultiplier, -1, isAccessible);
		}

		public void setSlotLimit(int slotLimit) {
			this.slotLimit = slotLimit;
		}

		public void setCompressible(boolean compressible) {
			isCompressible = compressible;
		}

		public ItemStack item() {
			return item;
		}

		public int prevSlotMultiplier() {
			return prevSlotMultiplier;
		}

		public int slotLimit() {
			return slotLimit;
		}

		public boolean isAccessible() {
			return isAccessible;
		}

		public boolean isCompressible() {
			return isCompressible;
		}

		@Override
		public String toString() {
			return "SlotDefinition{item=" + item + ", prevSlotMultiplier=" + prevSlotMultiplier + ", slotLimit=" + slotLimit
					+ ", isAccessible=" + isAccessible + ", isCompressible=" + isCompressible + '}';
		}
	}
}
