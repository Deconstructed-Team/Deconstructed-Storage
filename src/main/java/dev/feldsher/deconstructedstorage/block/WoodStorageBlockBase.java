package dev.feldsher.deconstructedstorage.block;

import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.WoodStorageBlockItem;
import dev.feldsher.deconstructedstorage.client.render.StorageParticles;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
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

	/** What its bits look like when broken, hit or run over: of its wood, or of its tinted core when it has none. */
	@SideOnly(Side.CLIENT)
	protected abstract TextureAtlasSprite getParticleSprite(@Nullable WoodType woodType);

	@SideOnly(Side.CLIENT)
	private boolean addParticles(IBlockAccess world, BlockPos pos, BiConsumer<TextureAtlasSprite, Integer> add) {
		StorageBlockEntity be = getStorageBlockEntity(world, pos);
		if (!(be instanceof WoodStorageBlockEntity)) {
			return false;
		}
		WoodType woodType = ((WoodStorageBlockEntity) be).getWoodType().orElse(null);
		StorageWrapper wrapper = be.getStorageWrapper();
		add.accept(getParticleSprite(woodType), woodType == null && wrapper.hasMainColor() ? wrapper.getMainColor() : -1);
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
		IBlockState state = world.getBlockState(pos);
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addDestroyEffects(world, pos, state, sprite, color, manager));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
		BlockPos pos = target.getBlockPos();
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addHitEffects(world, pos, target.sideHit, state, sprite, color, manager));
	}

	/** The server has nothing to show; the client throws bits of the storage's own wood. */
	@Override
	public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
		return !world.isRemote || addRunningParticles(state, world, pos, entity);
	}

	@SideOnly(Side.CLIENT)
	private boolean addRunningParticles(IBlockState state, World world, BlockPos pos, Entity entity) {
		return addParticles(world, pos, (sprite, color) -> StorageParticles.addRunningEffects(world, pos, state, entity, sprite, color));
	}
}
