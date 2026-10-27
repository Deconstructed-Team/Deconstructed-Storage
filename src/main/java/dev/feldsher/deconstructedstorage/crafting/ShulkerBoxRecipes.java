package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedstorage.block.ChestBlock;
import dev.feldsher.deconstructedstorage.item.ShulkerBoxItem;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;
import java.util.Optional;

/** Shulker boxes made from what's like them: a chest of the tier, keeping its name and colors, or vanilla's shulker box, with what it holds. */
public final class ShulkerBoxRecipes {
	private ShulkerBoxRecipes() {}

	private static Optional<ItemStack> find(InventoryCrafting inv, Class<? extends Block> blockClass) {
		for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
			ItemStack slotStack = inv.getStackInSlot(slot);
			if (blockClass.isInstance(Block.getBlockFromItem(slotStack.getItem()))) {
				return Optional.of(slotStack);
			}
		}
		return Optional.empty();
	}

	public static class FromChest extends ShapedOreRecipe {
		public FromChest(@Nullable ResourceLocation group, ItemStack result, CraftingHelper.ShapedPrimer primer) {
			super(group, result, primer);
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public ItemStack getCraftingResult(InventoryCrafting inv) {
			ItemStack shulker = super.getCraftingResult(inv);
			find(inv, ChestBlock.class).ifPresent(chest -> {
				if (chest.hasDisplayName()) {
					shulker.setStackDisplayName(chest.getDisplayName());
				}
				StorageBlockItem.getMainColor(chest).ifPresent(color -> StorageBlockItem.setMainColor(shulker, color));
				StorageBlockItem.getAccentColor(chest).ifPresent(color -> StorageBlockItem.setAccentColor(shulker, color));
			});
			return shulker;
		}
	}

	/**
	 * The vanilla box's items go along on the stack, to go into the new box's contents as it's placed: making the
	 * contents here would make them for every look at the result, as the crafting grid takes.
	 */
	public static class FromVanilla extends ShapelessOreRecipe {
		public FromVanilla(@Nullable ResourceLocation group, ItemStack result, ShapelessOreRecipe recipe) {
			super(group, recipe.getIngredients(), result);
		}

		@Override
		public boolean isDynamic() {
			return true;
		}

		@Override
		public ItemStack getCraftingResult(InventoryCrafting inv) {
			ItemStack shulker = super.getCraftingResult(inv);
			find(inv, BlockShulkerBox.class).ifPresent(vanilla -> {
				NBTTagCompound tag = vanilla.getTagCompound();
				NBTTagCompound blockEntityTag = tag == null ? null : tag.getCompoundTag("BlockEntityTag");
				if (blockEntityTag != null && blockEntityTag.hasKey("Items", Constants.NBT.TAG_LIST)) {
					ShulkerBoxItem.setPendingItems(shulker, blockEntityTag.getTagList("Items", Constants.NBT.TAG_COMPOUND));
				}
				if (vanilla.hasDisplayName()) {
					shulker.setStackDisplayName(vanilla.getDisplayName());
				}
			});
			return shulker;
		}
	}

	/** {@code "type": "deconstructedstorage:shulker_box_from_chest"}, the rest as {@code forge:ore_shaped}. */
	public static class FromChestFactory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			ShapedOreRecipe recipe = ShapedOreRecipe.factory(context, json);
			CraftingHelper.ShapedPrimer primer = new CraftingHelper.ShapedPrimer();
			primer.width = recipe.getRecipeWidth();
			primer.height = recipe.getRecipeHeight();
			primer.mirrored = JsonUtils.getBoolean(json, "mirrored", true);
			primer.input = recipe.getIngredients();
			String group = JsonUtils.getString(json, "group", "");
			return new FromChest(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), primer);
		}
	}

	/** {@code "type": "deconstructedstorage:shulker_box_from_vanilla"}, the rest as {@code forge:ore_shapeless}. */
	public static class FromVanillaFactory implements IRecipeFactory {
		@Override
		public IRecipe parse(JsonContext context, JsonObject json) {
			ShapelessOreRecipe recipe = ShapelessOreRecipe.factory(context, json);
			String group = JsonUtils.getString(json, "group", "");
			return new FromVanilla(group.isEmpty() ? null : new ResourceLocation(group), recipe.getRecipeOutput(), recipe);
		}
	}
}
