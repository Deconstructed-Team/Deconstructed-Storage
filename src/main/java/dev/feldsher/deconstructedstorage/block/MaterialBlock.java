package dev.feldsher.deconstructedstorage.block;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * A block whose textures go on a barrel: by its name and, as 1.12's blocks tell their variants apart by it, its
 * metadata, as white and orange wool are one block. Saved as "minecraft:wool@1", or only the name for metadata 0.
 */
public final class MaterialBlock {
	private final ResourceLocation blockName;
	private final int meta;

	public MaterialBlock(ResourceLocation blockName, int meta) {
		this.blockName = blockName;
		this.meta = meta;
	}

	/** The block of a stack that places one, as wool or planks. */
	public static Optional<MaterialBlock> of(ItemStack stack) {
		if (!(stack.getItem() instanceof ItemBlock)) {
			return Optional.empty();
		}
		Block block = ((ItemBlock) stack.getItem()).getBlock();
		return Optional.ofNullable(block.getRegistryName()).map(name -> new MaterialBlock(name, stack.getItem().getMetadata(stack.getMetadata())));
	}

	@Nullable
	public static MaterialBlock parse(String value) {
		int at = value.lastIndexOf('@');
		try {
			return at < 0 ? new MaterialBlock(new ResourceLocation(value), 0) : new MaterialBlock(new ResourceLocation(value.substring(0, at)), Integer.parseInt(value.substring(at + 1)));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** Its block, in the state its metadata says; none if no such block is registered. */
	@SuppressWarnings("deprecation")
	public Optional<IBlockState> getState() {
		Block block = ForgeRegistries.BLOCKS.getValue(blockName);
		if (block == null || !Objects.equals(block.getRegistryName(), blockName)) {
			return Optional.empty();
		}
		try {
			return Optional.of(block.getStateFromMeta(meta));
		} catch (RuntimeException e) {
			return Optional.of(block.getDefaultState());
		}
	}

	/** Whether this stack is of this block. */
	public boolean matches(ItemStack stack) {
		return of(stack).map(this::equals).orElse(false);
	}

	@Override
	public String toString() {
		return meta == 0 ? blockName.toString() : blockName + "@" + meta;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MaterialBlock)) {
			return false;
		}
		MaterialBlock that = (MaterialBlock) o;
		return meta == that.meta && blockName.equals(that.blockName);
	}

	@Override
	public int hashCode() {
		return blockName.hashCode() * 31 + meta;
	}
}
