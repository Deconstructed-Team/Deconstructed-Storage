package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.client.gui.utils.TranslationHelper;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import dev.feldsher.deconstructedstorage.item.StorageTierUpgradeItem;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A storage controller: placed, it takes in the storages around it, joined through each other, as one inventory. A
 * click on it deposits the held stack where that item already is; a quick second one, all such items the player has.
 */
public class ControllerBlock extends Block {
	public ControllerBlock() {
		super(Material.ROCK);
		setHardness(3F);
		// as 1.21's 6: 1.12 keeps three times what it's given
		setResistance(2F);
		setSoundType(SoundType.STONE);
		setHarvestLevel("pickaxe", 0);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		addTooltip(this, tooltip);
	}

	/** The block's lines, as "block.deconstructedstorage.controller.tooltip" says them. */
	@SideOnly(Side.CLIENT)
	static void addTooltip(Block block, List<String> tooltip) {
		for (String line : StorageTranslationHelper.INSTANCE.getTranslatedLines(block.getTranslationKey().replaceFirst("^tile\\.", "block.") + TranslationHelper.TOOLTIP_SUFFIX)) {
			tooltip.add(TextFormatting.DARK_GRAY + line);
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new ControllerBlockEntity();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		TileEntity te = world.getTileEntity(pos);
		if (!world.isRemote && te instanceof ControllerBlockEntity) {
			((ControllerBlockEntity) te).searchAndAddBoundables();
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ControllerBlockEntity) {
			((ControllerBlockEntity) te).detachFromStoragesAndUnlinkBlocks();
		}
		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		TileEntity te = world.getTileEntity(pos);
		if (te instanceof ControllerBlockEntity && !applyToStorages((ControllerBlockEntity) te, player, hand, false)) {
			((ControllerBlockEntity) te).depositPlayerItems(player, hand);
		}
		return true;
	}

	/**
	 * An upgrade in hand goes into each storage of the network, one into each, or sneaking as many as each takes; a tier
	 * upgrade item takes each a tier up. A double chest counts once.
	 */
	public static boolean applyToStorages(ControllerBlockEntity controller, EntityPlayer player, EnumHand hand, boolean fill) {
		ItemStack stack = player.getHeldItem(hand);
		if (stack.isEmpty()) {
			return false;
		}
		World world = controller.getWorld();
		boolean applied = false;
		Set<BlockPos> mainPositions = new HashSet<>();
		for (BlockPos storagePos : controller.getStoragePositions()) {
			TileEntity te = world.getTileEntity(storagePos);
			if (!(te instanceof StorageBlockEntity) || !mainPositions.add(((StorageBlockEntity) te).getMainBlockEntity().getPos())) {
				continue;
			}
			StorageBlockEntity be = (StorageBlockEntity) te;
			if (fill ? StorageBlockBase.tryFillUpgrades(player, hand, be) : StorageBlockBase.tryAddSingleUpgrade(player, hand, be)) {
				applied = true;
			} else if (stack.getItem() instanceof StorageTierUpgradeItem
					&& ((StorageTierUpgradeItem) stack.getItem()).tryUpgradeStorage(stack, world, storagePos, player) == EnumActionResult.SUCCESS) {
				applied = true;
			}
			if (stack.isEmpty()) {
				break;
			}
		}
		return applied;
	}
}
