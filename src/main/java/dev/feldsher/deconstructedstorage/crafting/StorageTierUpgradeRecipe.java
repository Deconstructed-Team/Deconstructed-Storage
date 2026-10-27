package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.item.ChestBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Turns a storage item into the next tier's: the result keeps all the stack had, its wood, colors and name, and for a
 * shulker box what it holds, with the new tier's slot counts, which the box placed grows to.
 */
public final class StorageTierUpgradeRecipe {
	private StorageTierUpgradeRecipe() {}

	static ItemStack upgrade(ItemStack result, InventoryCrafting inv) {
		getOriginalStorage(inv).filter(storage -> storage.getTagCompound() != null)
				.ifPresent(storage -> result.setTagCompound(storage.getTagCompound().copy()));
		Block block = Block.getBlockFromItem(result.getItem());
		if (StorageBlockItem.getContentsUuid(result).isPresent() && block instanceof StorageBlockBase) {
			StorageBlockBase storageBlock = (StorageBlockBase) block;
			StorageBlockItem.getContentsUuid(result).ifPresent(uuid ->
					StorageBlockItem.setContents(result, uuid, storageBlock.getNumberOfInventorySlots(), storageBlock.getNumberOfUpgradeSlots()));
		}
		return result;
	}

	/** A packed double chest isn't one storage of the tier: it goes up a tier only placed, with an upgrade item. */
	static boolean isNotDoubleChest(InventoryCrafting inv) {
		return getOriginalStorage(inv).map(storage -> !ChestBlockItem.isDoubleChest(storage)).orElse(false);
	}

	private static Optional<ItemStack> getOriginalStorage(InventoryCrafting inv) {
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (slotStack.getItem() instanceof StorageBlockItem) {
				return Optional.of(slotStack);
			}
		}
		return Optional.empty();
	}

	public static class Shaped extends ShapedOreRecipe {
		public Shaped(@Nullable ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
			super(group, result, primer);
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public boolean matches(InventoryCrafting inv, World world) {
			return super.matches(inv, world) && isNotDoubleChest(inv);
		}

		@Override
		public ItemStack getCraftingResult(InventoryCrafting inv) {
			return upgrade(super.getCraftingResult(inv), inv);
		}
	}

	public static class Shapeless extends ShapelessOreRecipe {
		public Shapeless(@Nullable ResourceLocation group, ItemStack result, ShapelessOreRecipe recipe) {
			super(group, recipe.getIngredients(), result);
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public boolean matches(InventoryCrafting inv, World world) {
			return super.matches(inv, world) && isNotDoubleChest(inv);
		}

		@Override
		public ItemStack getCraftingResult(InventoryCrafting inv) {
			return upgrade(super.getCraftingResult(inv), inv);
		}
	}

	/** {@code "type": "deconstructedstorage:storage_tier_upgrade"}, the rest as {@code forge:ore_shaped}. */
	public static class ShapedFactory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);
			CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
			primer.width = recipe.getRecipeWidth();
			primer.height = recipe.getRecipeHeight();
			primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
			primer.input = recipe.getIngredients();
			String group = JsonUtils.getString(json, "group", "");
			return new Shaped(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), primer);
		}
	}

	/** {@code "type": "deconstructedstorage:storage_tier_upgrade_shapeless"}, the rest as {@code forge:ore_shapeless}. */
	public static class ShapelessFactory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			ShapelessOreRecipe recipe = ShapelessOreRecipe.factory(context, json);
			String group = JsonUtils.getString(json, "group", "");
			return new Shapeless(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), recipe);
		}
	}
}
