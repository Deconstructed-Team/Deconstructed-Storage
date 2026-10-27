package dev.feldsher.deconstructedstorage.client.render;

import dev.feldsher.deconstructedstorage.block.BarrelRenderData;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Breaking, hitting and running over a storage throw bits of what it's made of, as 1.21's tinted terrain particles: its
 * wood, or, tinted, its main color. 1.12 takes them from the model's one particle texture, which can't know the wood.
 */
public final class StorageParticles {
	private StorageParticles() {}

	private static final class Bit extends ParticleDigging {
		/** The block's position gives the light it's drawn in: without it, the bit can't be drawn. */
		Bit(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, IBlockState state, BlockPos pos, TextureAtlasSprite sprite,
				int color) {
			super(world, x, y, z, xSpeed, ySpeed, zSpeed, state);
			setBlockPos(pos);
			setParticleTexture(sprite);
			if (color != -1) {
				setRBGColorF(0.6F * (color >> 16 & 255) / 255F, 0.6F * (color >> 8 & 255) / 255F, 0.6F * (color & 255) / 255F);
			}
		}
	}

	private static TextureAtlasSprite getSprite(BarrelRenderData data) {
		return BarrelParts.getParticleSprite(Minecraft.getMinecraft().getTextureMapBlocks(), data.getWoodType());
	}

	private static int getColor(BarrelRenderData data, StorageWrapper wrapper) {
		return data.getWoodType() == null && data.hasMainColor() ? wrapper.getMainColor() : -1;
	}

	/** As ParticleManager.addBlockDestroyEffects: a 4x4x4 of bits. */
	public static void addDestroyEffects(World world, BlockPos pos, IBlockState state, BarrelRenderData data, StorageWrapper wrapper, ParticleManager manager) {
		TextureAtlasSprite sprite = getSprite(data);
		int color = getColor(data, wrapper);
		for (int i = 0; i < 4; ++i) {
			for (int j = 0; j < 4; ++j) {
				for (int k = 0; k < 4; ++k) {
					double x = (i + 0.5D) / 4.0D;
					double y = (j + 0.5D) / 4.0D;
					double z = (k + 0.5D) / 4.0D;
					manager.addEffect(new Bit(world, pos.getX() + x, pos.getY() + y, pos.getZ() + z, x - 0.5D, y - 0.5D, z - 0.5D, state, pos, sprite, color));
				}
			}
		}
	}

	/** As ParticleManager.addBlockHitEffects: a bit off the face that was hit. */
	public static void addHitEffects(World world, BlockPos pos, EnumFacing side, IBlockState state, BarrelRenderData data, StorageWrapper wrapper, ParticleManager manager) {
		AxisAlignedBB box = state.getBoundingBox(world, pos);
		double x = pos.getX() + world.rand.nextDouble() * (box.maxX - box.minX - 0.2D) + 0.1D + box.minX;
		double y = pos.getY() + world.rand.nextDouble() * (box.maxY - box.minY - 0.2D) + 0.1D + box.minY;
		double z = pos.getZ() + world.rand.nextDouble() * (box.maxZ - box.minZ - 0.2D) + 0.1D + box.minZ;
		switch (side) {
			case DOWN:
				y = pos.getY() + box.minY - 0.1D;
				break;
			case UP:
				y = pos.getY() + box.maxY + 0.1D;
				break;
			case NORTH:
				z = pos.getZ() + box.minZ - 0.1D;
				break;
			case SOUTH:
				z = pos.getZ() + box.maxZ + 0.1D;
				break;
			case WEST:
				x = pos.getX() + box.minX - 0.1D;
				break;
			default:
				x = pos.getX() + box.maxX + 0.1D;
				break;
		}
		manager.addEffect(new Bit(world, x, y, z, 0, 0, 0, state, pos, getSprite(data), getColor(data, wrapper)).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
	}

	/** As Entity.createRunningParticles: a bit kicked up under the entity's feet. */
	public static void addRunningEffects(World world, BlockPos pos, IBlockState state, Entity entity, BarrelRenderData data, StorageWrapper wrapper) {
		Minecraft.getMinecraft().effectRenderer.addEffect(new Bit(world,
				entity.posX + (world.rand.nextFloat() - 0.5D) * entity.width, entity.getEntityBoundingBox().minY + 0.1D,
				entity.posZ + (world.rand.nextFloat() - 0.5D) * entity.width,
				-entity.motionX * 4.0D, 1.5D, -entity.motionZ * 4.0D, state, pos, getSprite(data), getColor(data, wrapper)));
	}
}
