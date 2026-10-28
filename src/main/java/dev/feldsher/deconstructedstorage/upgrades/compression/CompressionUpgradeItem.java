package dev.feldsher.deconstructedstorage.upgrades.compression;

import dev.feldsher.deconstructedcore.api.IStorageWrapper;
import dev.feldsher.deconstructedcore.common.gui.UpgradeSlotChangeResult;
import dev.feldsher.deconstructedcore.inventory.InventoryHandler;
import dev.feldsher.deconstructedcore.inventory.InventoryPartRegistry;
import dev.feldsher.deconstructedcore.inventory.InventoryPartitioner;
import dev.feldsher.deconstructedcore.inventory.SlotRange;
import dev.feldsher.deconstructedcore.settings.memory.MemorySettingsCategory;
import dev.feldsher.deconstructedcore.upgrades.IUpgradeItem;
import dev.feldsher.deconstructedcore.upgrades.UpgradeItemBase;
import dev.feldsher.deconstructedcore.upgrades.UpgradeType;
import dev.feldsher.deconstructedcore.upgrades.UpgradeWrapperBase;
import dev.feldsher.deconstructedcore.upgrades.compacting.CompactingUpgradeItem;
import dev.feldsher.deconstructedcore.util.RecipeHelper;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Takes over the first free run of up to so many inventory slots with a {@link CompressionInventoryPart}. It goes in
 * only if what those slots hold already forms a chain, each slot what the one before compresses into.
 */
public class CompressionUpgradeItem extends UpgradeItemBase<CompressionUpgradeItem.Wrapper> {
	public static final UpgradeType<Wrapper> TYPE = new UpgradeType<>(Wrapper::new);
	private static final String FIRST_INVENTORY_SLOT_TAG = "firstInventorySlot";
	private static final List<UpgradeConflictDefinition> UPGRADE_CONFLICT_DEFINITIONS = Collections.singletonList(
			new UpgradeConflictDefinition(CompactingUpgradeItem.class::isInstance, 0, StorageTranslationHelper.INSTANCE.translError("add.compacting_exists")));

	public CompressionUpgradeItem() {
		super(Config.SERVER.maxUpgradesPerStorageConfig);
		InventoryPartRegistry.registerFactory(CompressionInventoryPart.NAME, CompressionInventoryPart::new);
	}

	@Override
	public UpgradeType<Wrapper> getType() {
		return TYPE;
	}

	private UpgradeSlotChangeResult checkCompressionSpace(IStorageWrapper storageWrapper) {
		Optional<SlotRange> slotRange = storageWrapper.getInventoryHandler().getInventoryPartitioner().getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots);

		return slotRange.map(range -> canUseForCompression(storageWrapper, range))
				.orElseGet(() -> UpgradeSlotChangeResult.fail(StorageTranslationHelper.INSTANCE.translError("add.compression_no_space"),
						Collections.emptySet(), Collections.emptySet(), Collections.emptySet()));
	}

	@Override
	public UpgradeSlotChangeResult checkExtraInsertConditions(ItemStack upgradeStack, IStorageWrapper storageWrapper, boolean isClientSide, @Nullable IUpgradeItem<?> upgradeInSlot) {
		if (isClientSide) {
			return UpgradeSlotChangeResult.success();
		}

		return checkCompressionSpace(storageWrapper);
	}

	@Override
	public List<UpgradeConflictDefinition> getUpgradeConflicts() {
		return UPGRADE_CONFLICT_DEFINITIONS;
	}

	private UpgradeSlotChangeResult canUseForCompression(IStorageWrapper storageWrapper, SlotRange slotRange) {
		boolean allRemainingSlotsMustBeEmpty = false;
		ItemStack nextItemToMatch = ItemStack.EMPTY;
		Set<Integer> errorSlots = new LinkedHashSet<>();
		InventoryHandler inventoryHandler = storageWrapper.getInventoryHandler();
		MemorySettingsCategory memorySettingsCategory = storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class);
		for (int slot = slotRange.firstSlot() + slotRange.numberOfSlots() - 1; slot >= slotRange.firstSlot(); slot--) {
			ItemStack stackToMatch;
			ItemStack slotStack = inventoryHandler.getSlotStack(slot);
			if (!slotStack.isEmpty()) {
				stackToMatch = slotStack;
			} else {
				stackToMatch = memorySettingsCategory.getSlotFilterStack(slot, false).orElse(ItemStack.EMPTY);
			}
			if (!stackToMatch.isEmpty()) {
				if (allRemainingSlotsMustBeEmpty) {
					errorSlots.add(slot);
				} else {
					if (!nextItemToMatch.isEmpty() && !isSameItem(nextItemToMatch, stackToMatch)) {
						errorSlots.add(slot);
						break;
					}

					boolean hasSlotBeforeThisOne = slot - 1 >= slotRange.firstSlot();
					if (hasSlotBeforeThisOne) {
						RecipeHelper.CompactingShape compactingShape = RecipeHelper.getItemCompactingShapes(stackToMatch).stream()
								.filter(CompressionUpgradeItem::isUncraftable).findFirst().orElse(RecipeHelper.CompactingShape.NONE);
						if (compactingShape == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE || compactingShape == RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE) {
							nextItemToMatch = RecipeHelper.getCompactingResult(stackToMatch, compactingShape).getResult();
						} else {
							allRemainingSlotsMustBeEmpty = true;
						}
					}
				}
			}
		}

		return !errorSlots.isEmpty() ? UpgradeSlotChangeResult.fail(StorageTranslationHelper.INSTANCE.translError("add.compression_incompatible_items"),
				Collections.emptySet(), errorSlots, Collections.emptySet()) : UpgradeSlotChangeResult.success();
	}

	private static boolean isUncraftable(RecipeHelper.CompactingShape shape) {
		return shape == RecipeHelper.CompactingShape.TWO_BY_TWO_UNCRAFTABLE || shape == RecipeHelper.CompactingShape.THREE_BY_THREE_UNCRAFTABLE;
	}

	private static boolean isSameItem(ItemStack a, ItemStack b) {
		return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata() && ItemStack.areItemStackTagsEqual(a, b);
	}

	@Override
	public ItemStack getCleanedUpgradeStack(ItemStack upgradeStack) {
		NBTTagCompound tag = upgradeStack.getTagCompound();
		if (tag != null) {
			tag.removeTag(FIRST_INVENTORY_SLOT_TAG);
			if (tag.isEmpty()) {
				upgradeStack.setTagCompound(null);
			}
		}
		return upgradeStack;
	}

	public static class Wrapper extends UpgradeWrapperBase<Wrapper, CompressionUpgradeItem> {
		protected Wrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
			super(storageWrapper, upgrade, upgradeSaveHandler);
		}

		@Override
		public void onAdded() {
			upgradeItem.getCleanedUpgradeStack(upgrade);
			InventoryPartitioner inventoryPartitioner = storageWrapper.getInventoryHandler().getInventoryPartitioner();
			inventoryPartitioner.getFirstSpace(Config.SERVER.compressionUpgrade.maxNumberOfSlots).ifPresent(slotRange -> {
				setFirstInventorySlot(slotRange.firstSlot());
				inventoryPartitioner.addInventoryPart(slotRange.firstSlot(), slotRange.numberOfSlots(), new CompressionInventoryPart(storageWrapper.getInventoryHandler(),
						slotRange, () -> storageWrapper.getSettingsHandler().getTypeCategory(MemorySettingsCategory.class)));
			});
		}

		private void setFirstInventorySlot(int firstInventorySlot) {
			NBTTagCompound tag = upgrade.getTagCompound();
			if (tag == null) {
				tag = new NBTTagCompound();
				upgrade.setTagCompound(tag);
			}
			tag.setInteger(FIRST_INVENTORY_SLOT_TAG, firstInventorySlot);
			save();
		}

		@Override
		public boolean canBeDisabled() {
			return false;
		}

		@Override
		public boolean hideSettingsTab() {
			return true;
		}

		@Override
		public void onBeforeRemoved() {
			super.onBeforeRemoved();
			storageWrapper.getInventoryHandler().getInventoryPartitioner().removeInventoryPart(getFirstInventorySlot());
			save();
			// taken out, it's a plain upgrade again, which stacks with others: the stack taken out is this one
			upgradeItem.getCleanedUpgradeStack(upgrade);
		}

		private int getFirstInventorySlot() {
			NBTTagCompound tag = upgrade.getTagCompound();
			return tag != null && tag.hasKey(FIRST_INVENTORY_SLOT_TAG) ? tag.getInteger(FIRST_INVENTORY_SLOT_TAG) : -1;
		}
	}
}
