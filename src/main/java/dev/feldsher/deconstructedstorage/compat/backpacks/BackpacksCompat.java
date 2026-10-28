package dev.feldsher.deconstructedstorage.compat.backpacks;

import dev.feldsher.deconstructedbackpacks.backpack.BackpackItem;
import dev.feldsher.deconstructedbackpacks.backpack.wrapper.BackpackWrapper;
import dev.feldsher.deconstructedbackpacks.backpack.wrapper.IBackpackWrapper;
import dev.feldsher.deconstructedstorage.block.BarrelMaterial;
import dev.feldsher.deconstructedstorage.block.DecorationTableBlockEntity;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.util.DecorationHelper;
import net.minecraft.item.ItemStack;

import java.util.Map;

/** With Deconstructed Backpacks there: the decoration table dyes backpacks, their cloth and trim, with 20 and 4 parts of dyes. */
public final class BackpacksCompat {
	public static final String MOD_ID = "deconstructedbackpacks";

	private BackpacksCompat() {}

	public static void init() {
		DecorationTableBlockEntity.registerItemDecorator(stack -> stack.getItem() instanceof BackpackItem, new DecorationTableBlockEntity.IItemDecorator() {
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
			public DecorationTableBlockEntity.TintDecorationResult decorateWithTints(ItemStack input, int mainColorToSet, int accentColorToSet) {
				IBackpackWrapper backpackWrapper = BackpackWrapper.fromStack(input);
				int originalMainColor = backpackWrapper.getMainColor() & 0xFFFFFF;
				int originalAccentColor = backpackWrapper.getAccentColor() & 0xFFFFFF;
				if ((mainColorToSet == -1 || mainColorToSet == originalMainColor) && (accentColorToSet == -1 || accentColorToSet == originalAccentColor)) {
					return DecorationTableBlockEntity.TintDecorationResult.EMPTY;
				}

				ItemStack result = input.copy();
				result.setCount(1);
				// a transparent color leaves that part as it is
				BackpackItem.setColors(result, mainColorToSet == -1 ? backpackWrapper.getMainColor() : mainColorToSet,
						accentColorToSet == -1 ? backpackWrapper.getAccentColor() : accentColorToSet);
				return new DecorationTableBlockEntity.TintDecorationResult(result,
						DecorationHelper.getDyePartsNeeded(mainColorToSet, accentColorToSet, originalMainColor, originalAccentColor, 20, 4));
			}
		});
	}
}
