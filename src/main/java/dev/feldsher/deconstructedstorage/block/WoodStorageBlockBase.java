package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedcore.util.InventoryHelper;
import dev.feldsher.deconstructedstorage.Config;
import dev.feldsher.deconstructedstorage.client.gui.StorageTranslationHelper;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.function.IntSupplier;

/** A storage made of wood: of each of the woods, or tinted, as its stack says. */
public abstract class WoodStorageBlockBase extends StorageBlockBase {
	protected WoodStorageBlockBase(IntSupplier numberOfInventorySlotsSupplier, IntSupplier numberOfUpgradeSlotsSupplier) {
		super(Material.WOOD, numberOfInventorySlotsSupplier, numberOfUpgradeSlotsSupplier);
	}

	@Override
	protected void addDropData(ItemStack stack, StorageBlockEntity be) {
		super.addDropData(stack, be);
		if (be instanceof WoodStorageBlockEntity) {
			WoodStorageBlockEntity wbe = (WoodStorageBlockEntity) be;
			StorageWrapper storageWrapper = wbe.getStorageWrapper();
			if (storageWrapper.hasMainColor()) {
				StorageBlockItem.setMainColor(stack, storageWrapper.getMainColor());
			}
			if (storageWrapper.hasAccentColor()) {
				StorageBlockItem.setAccentColor(stack, storageWrapper.getAccentColor());
			}
			wbe.getWoodType().ifPresent(woodType -> WoodStorageBlockItem.setWoodType(stack, woodType));
		}
	}

	@Override
	protected void setPlacedData(ItemStack stack, StorageBlockEntity be) {
		super.setPlacedData(stack, be);
		if (be instanceof WoodStorageBlockEntity) {
			WoodStorageBlockEntity wbe = (WoodStorageBlockEntity) be;
			wbe.getStorageWrapper().setColors(StorageBlockItem.getMainColor(stack).orElse(-1), StorageBlockItem.getAccentColor(stack).orElse(-1));
			wbe.setWoodType(WoodStorageBlockItem.getWoodType(stack).orElse(null));
		}
	}

	/** Each wood, then, for the basic tier, a tinted one of each dye color, and one yellow and lime. */
	@Override
	public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
		for (WoodType woodType : WoodType.values()) {
			items.add(WoodStorageBlockItem.setWoodType(new ItemStack(this), woodType));
		}
		if (isBasicTier()) {
			for (EnumDyeColor color : EnumDyeColor.values()) {
				items.add(StorageBlockItem.setColors(new ItemStack(this), color.getColorValue(), color.getColorValue()));
			}
			items.add(StorageBlockItem.setColors(new ItemStack(this), EnumDyeColor.YELLOW.getColorValue(), EnumDyeColor.LIME.getColorValue()));
		}
	}

	protected abstract boolean isBasicTier();

	/**
	 * Packed, it goes with what it holds; with the config's dropPacked, so does any that holds something. A double
	 * chest's halves, unpacked, part first: each then drops as a plain chest.
	 */
	@Override
	protected boolean dropsWithContents(StorageBlockEntity be) {
		if (!(be instanceof WoodStorageBlockEntity)) {
			return false;
		}
		if (((WoodStorageBlockEntity) be).isPacked()) {
			return true;
		}
		return Config.SERVER.dropPacked && be.getMainBlockEntity() == be && be.getStorageWrapper().getContentsUuid().isPresent()
				&& !(InventoryHelper.isEmpty(be.getStorageWrapper().getInventoryHandler()) && InventoryHelper.isEmpty(be.getStorageWrapper().getUpgradeHandler()));
	}

	/** Packing tape packs it; a packed one opens to nobody. */
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!(be instanceof WoodStorageBlockEntity)) {
			return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
		}
		if (((WoodStorageBlockEntity) be.getMainBlockEntity()).isPacked()) {
			return true;
		}
		ItemStack held = player.getHeldItem(hand);
		if (held.getItem() == ModItems.PACKING_TAPE) {
			if (!world.isRemote) {
				if (Config.SERVER.dropPacked) {
					player.sendStatusMessage(new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translGui("status.packing_tape_disabled")), true);
				} else {
					packStorage(world, pos, (WoodStorageBlockEntity) be);
					if (!player.capabilities.isCreativeMode) {
						held.damageItem(1, player);
					}
				}
			}
			return true;
		}
		return super.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
	}

	protected void packStorage(World world, BlockPos pos, WoodStorageBlockEntity be) {
		be.setPacked(true);
	}

	/** What its bits look like when broken, hit or run over: of its wood, or of its tinted core when it has none. */
	@SideOnly(Side.CLIENT)
	protected abstract TextureAtlasSprite getParticleSprite(@Nullable WoodType woodType);

	@SideOnly(Side.CLIENT)
	@Override
	protected TextureAtlasSprite getParticleSprite(StorageBlockEntity be) {
		return getParticleSprite(be instanceof WoodStorageBlockEntity ? ((WoodStorageBlockEntity) be).getWoodType().orElse(null) : WoodType.DEFAULT);
	}

	@Override
	protected int getParticleColor(StorageBlockEntity be) {
		boolean woodless = be instanceof WoodStorageBlockEntity && !((WoodStorageBlockEntity) be).getWoodType().isPresent();
		return woodless && be.getStorageWrapper().hasMainColor() ? be.getStorageWrapper().getMainColor() : -1;
	}
}
