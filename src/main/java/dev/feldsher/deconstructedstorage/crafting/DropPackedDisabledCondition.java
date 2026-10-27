package dev.feldsher.deconstructedstorage.crafting;

import com.google.gson.JsonObject;
import dev.feldsher.deconstructedstorage.Config;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/** {@code {"type": "deconstructedstorage:drop_packed_disabled"}}: packing tape is of no use when every storage drops packed. */
public class DropPackedDisabledCondition implements IConditionFactory {
	@Override
	public BooleanSupplier parse(JsonContext context, JsonObject json) {
		return () -> !Config.SERVER.dropPacked;
	}
}
