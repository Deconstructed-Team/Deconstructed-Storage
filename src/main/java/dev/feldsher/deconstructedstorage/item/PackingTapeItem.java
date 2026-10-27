package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedstorage.Config;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Packs a barrel or chest it's used on: broken, the storage then goes as a stack with all it holds. Eight uses; none
 * with the config's dropPacked, which packs every storage that holds something anyway.
 */
public class PackingTapeItem extends Item {
	public PackingTapeItem() {
		setMaxStackSize(1);
		setMaxDamage(8);
	}

	@SuppressWarnings("deprecation") // the client's own translation here
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		if (Config.SERVER.dropPacked) {
			tooltip.add(TextFormatting.RED + I18n.translateToLocal(getTranslationKey() + ".tooltip.disabled"));
		} else {
			tooltip.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(getTranslationKey() + ".tooltip",
					TextFormatting.WHITE.toString() + (stack.getMaxDamage() - stack.getItemDamage()) + TextFormatting.GRAY));
		}
		super.addInformation(stack, world, tooltip, flag);
	}
}
