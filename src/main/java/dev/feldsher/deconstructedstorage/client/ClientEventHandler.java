package dev.feldsher.deconstructedstorage.client;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.BarrelBlock;
import dev.feldsher.deconstructedstorage.block.StorageBlockEntity;
import dev.feldsher.deconstructedstorage.block.StorageWrapper;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlock;
import dev.feldsher.deconstructedstorage.block.VerticalFacing;
import dev.feldsher.deconstructedstorage.client.render.BarrelBakedModel;
import dev.feldsher.deconstructedstorage.client.render.BarrelParts;
import dev.feldsher.deconstructedstorage.client.render.ChestItemRenderer;
import dev.feldsher.deconstructedstorage.init.ModBlocks;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.item.StorageBlockItem;
import dev.feldsher.deconstructedstorage.item.StorageToolItem;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import dev.feldsher.deconstructedstorage.network.ScrolledToolMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import dev.feldsher.deconstructedstorage.upgrades.compression.CompressionInventoryPart;
import dev.feldsher.deconstructedstorage.upgrades.hopper.HopperUpgradeContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Objects;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID, value = Side.CLIENT)
public class ClientEventHandler {
	private ClientEventHandler() {}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		for (Block block : ModBlocks.getAllBlocks()) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, new ModelResourceLocation(Objects.requireNonNull(block.getRegistryName()), "inventory"));
		}
		ModelLoader.setCustomModelResourceLocation(ModItems.STORAGE_TOOL, 0, new ModelResourceLocation(Objects.requireNonNull(ModItems.STORAGE_TOOL.getRegistryName()), "inventory"));
		ModelLoader.setCustomModelResourceLocation(ModItems.PACKING_TAPE, 0, new ModelResourceLocation(Objects.requireNonNull(ModItems.PACKING_TAPE.getRegistryName()), "inventory"));
		ModelLoader.setCustomModelResourceLocation(ModItems.UPGRADE_BASE, 0, new ModelResourceLocation(Objects.requireNonNull(ModItems.UPGRADE_BASE.getRegistryName()), "inventory"));
		for (Item item : ModItems.STORAGE_UPGRADES) {
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
		}
		for (Item item : ModItems.getTierUpgrades()) {
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
		}
		ChestItemRenderer chestItemRenderer = new ChestItemRenderer();
		ModBlocks.getChests().forEach(chest -> Item.getItemFromBlock(chest).setTileEntityItemStackRenderer(chestItemRenderer));
		ModBlocks.getShulkerBoxes().forEach(box -> Item.getItemFromBlock(box).setTileEntityItemStackRenderer(chestItemRenderer));
	}

	/** Sneak and scroll with the storage tool in hand changes its mode, here at once and on the server, and says the new one. */
	@SubscribeEvent
	public static void onMouseScroll(MouseEvent event) {
		Minecraft minecraft = Minecraft.getMinecraft();
		EntityPlayerSP player = minecraft.player;
		if (event.getDwheel() == 0 || player == null || !player.isSneaking() || minecraft.currentScreen != null) {
			return;
		}
		ItemStack stack = player.getHeldItemMainhand();
		if (!(stack.getItem() instanceof StorageToolItem)) {
			return;
		}
		boolean next = event.getDwheel() < 0;
		StorageToolItem.cycleMode(stack, next);
		PacketHandler.INSTANCE.sendToServer(new ScrolledToolMessage(next));
		minecraft.ingameGUI.setOverlayMessage(StorageToolItem.getOverlayMessage(stack), false);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onTextureStitch(TextureStitchEvent.Pre event) {
		BarrelParts.registerSprites(event.getMap());
		event.getMap().registerSprite(new ResourceLocation(HopperUpgradeContainer.EMPTY_INPUT_FILTER_SLOT_BACKGROUND));
		event.getMap().registerSprite(new ResourceLocation(CompressionInventoryPart.EMPTY_COMPRESSION_SLOT));
		event.getMap().registerSprite(new ResourceLocation(HopperUpgradeContainer.EMPTY_OUTPUT_FILTER_SLOT_BACKGROUND));
	}

	/** Barrels get put together from their parts, placed by their render data, as items by their stacks. */
	@SubscribeEvent
	public static void onModelBake(ModelBakeEvent event) {
		BarrelParts.clear();
		for (BarrelBlock barrel : ModBlocks.getBarrels()) {
			for (EnumFacing facing : EnumFacing.values()) {
				for (boolean flatTop : new boolean[] {false, true}) {
					wrap(event, new ModelResourceLocation(Objects.requireNonNull(barrel.getRegistryName()), "facing=" + facing.getName() + ",flat_top=" + flatTop), barrel,
							BarrelParts.getRotation(facing), flatTop);
				}
			}
			wrap(event, new ModelResourceLocation(Objects.requireNonNull(barrel.getRegistryName()), "inventory"), barrel, ModelRotation.X0_Y0, false);
		}
		for (LimitedBarrelBlock barrel : ModBlocks.getLimitedBarrels()) {
			for (EnumFacing facing : EnumFacing.HORIZONTALS) {
				for (VerticalFacing verticalFacing : VerticalFacing.values()) {
					for (boolean flatTop : new boolean[] {false, true}) {
						wrap(event, new ModelResourceLocation(Objects.requireNonNull(barrel.getRegistryName()),
										"facing=" + facing.getName() + ",flat_top=" + flatTop + ",vertical_facing=" + verticalFacing.getName()), barrel,
								BarrelParts.getLimitedRotation(facing, verticalFacing), flatTop);
					}
				}
			}
			wrap(event, new ModelResourceLocation(Objects.requireNonNull(barrel.getRegistryName()), "inventory"), barrel, ModelRotation.X0_Y0, false);
		}
	}

	private static void wrap(ModelBakeEvent event, ModelResourceLocation location, Block barrel, ModelRotation rotation, boolean flatTop) {
		IBakedModel model = event.getModelRegistry().getObject(location);
		if (model != null && !(model instanceof BarrelBakedModel)) {
			event.getModelRegistry().putObject(location, new BarrelBakedModel(model, barrel, rotation, flatTop));
		}
	}

	@SubscribeEvent
	public static void registerBlockColors(ColorHandlerEvent.Block event) {
		event.getBlockColors().registerBlockColorHandler((state, world, pos, tintIndex) -> {
			if (world == null || pos == null) {
				return -1;
			}
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof StorageBlockEntity)) {
				return -1;
			}
			StorageWrapper wrapper = ((StorageBlockEntity) te).getStorageWrapper();
			if (tintIndex == BarrelParts.MAIN_TINT) {
				return wrapper.getMainColor();
			}
			return tintIndex == BarrelParts.ACCENT_TINT ? wrapper.getAccentColor() : -1;
		}, ModBlocks.getAllBlocks().toArray(new Block[0]));
	}

	@SubscribeEvent
	public static void registerItemColors(ColorHandlerEvent.Item event) {
		event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
			Optional<Integer> color = tintIndex == BarrelParts.MAIN_TINT ? StorageBlockItem.getMainColor(stack)
					: tintIndex == BarrelParts.ACCENT_TINT ? StorageBlockItem.getAccentColor(stack) : Optional.empty();
			return color.orElse(-1);
		}, ModBlocks.getAllBlocks().stream().map(Item::getItemFromBlock).toArray(Item[]::new));
	}
}
