package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.function.BooleanSupplier;

/**
 * {@code {"type": "deconstructedstorage:item_enabled", "item": "deconstructedstorage:copper_barrel"}}: a recipe of an
 * item the config may leave out, as a tier or a kind of storage, which isn't registered then.
 */
public class ItemEnabledCondition implements IConditionFactory {
	@Override
	public BooleanSupplier parse(JsonContext context, JsonObject json) {
		ResourceLocation item = new ResourceLocation(JsonUtils.getString(json, "item"));
		return () -> ForgeRegistries.ITEMS.containsKey(item);
	}
}
