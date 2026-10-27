package dev.feldsher.deconstructedstorage.item;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedstorage.block.StorageTier;
import dev.feldsher.deconstructedstorage.block.TierUpgrader;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Used on a placed storage, upgrades it to another tier where it stands, with all it holds. The basic tier upgrade
 * turns vanilla's chests and shulker boxes into these; the others take a storage of their from tier, and from the
 * basic tier vanilla's as well, straight to their to tier.
 */
public class StorageTierUpgradeItem extends Item {
	@Nullable
	private final StorageTier from;
	private final StorageTier to;

	/** @param from the tier upgraded from, or null for vanilla's storages only */
	public StorageTierUpgradeItem(@Nullable StorageTier from, StorageTier to) {
		this.from = from;
		this.to = to;
	}

	/** Whether the tiers it's between exist: the config may leave copper or netherite out. */
	public boolean isEnabled() {
		return (from == null || from.isEnabled()) && to.isEnabled();
	}

	@Override
	public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
		if (world.isRemote) {
			return EnumActionResult.PASS;
		}
		Block block = world.getBlockState(pos).getBlock();
		Block newBlock = TierUpgrader.getUpgradedBlock(block, from, to);
		if (newBlock == null || TierUpgrader.isUpgradingBlocked(world, pos)) {
			return EnumActionResult.PASS;
		}
		ItemStack stack = player.getHeldItem(hand);
		int countRequired = TierUpgrader.getCountRequired(world, pos);
		if (countRequired > stack.getCount()) {
			player.sendStatusMessage(new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translGui("status.too_low_tier_upgrade_count"),
					countRequired, stack.getDisplayName()), true);
			return EnumActionResult.FAIL;
		}
		boolean upgraded;
		if (block instanceof BlockChest) {
			upgraded = TierUpgrader.upgradeVanillaChest(world, pos, newBlock);
		} else if (block instanceof BlockShulkerBox) {
			upgraded = TierUpgrader.upgradeVanillaShulkerBox(world, pos, newBlock);
		} else {
			upgraded = TierUpgrader.upgradeStorage(world, pos, newBlock);
		}
		if (!upgraded) {
			return EnumActionResult.PASS;
		}
		if (!player.capabilities.isCreativeMode) {
			stack.shrink(countRequired);
		}
		return EnumActionResult.SUCCESS;
	}

	/** The basic one says what it's for: the rest are named after their tiers. */
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		if (from == null) {
			for (String line : StorageTranslationHelper.INSTANCE.getTranslatedLines(getTranslationKey() + TranslationHelper.TOOLTIP_SUFFIX)) {
				tooltip.add(TextFormatting.DARK_GRAY + line);
			}
		}
		super.addInformation(stack, world, tooltip, flag);
	}
}
