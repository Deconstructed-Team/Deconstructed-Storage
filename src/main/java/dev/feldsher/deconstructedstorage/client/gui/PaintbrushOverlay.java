package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.ControllerBlock;
import dev.feldsher.deconstructedstorage.block.StorageBlockBase;
import dev.feldsher.deconstructedstorage.init.ModItems;
import dev.feldsher.deconstructedstorage.item.PaintbrushItem;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Optional;

/** Looking at a storage or a controller with a set up paintbrush in hand: what the player lacks for painting it, above the hotbar. */
@Mod.EventBusSubscriber(modid = DeconstructedStorage.MOD_ID, value = Side.CLIENT)
public final class PaintbrushOverlay {
	private static Optional<PaintbrushItem.ItemRequirements> itemRequirementsCache = Optional.empty();
	@Nullable
	private static BlockPos lastPosCached = null;
	@Nullable
	private static ItemStack lastPaintbrushCached = null;

	private PaintbrushOverlay() {}

	private static Optional<PaintbrushItem.ItemRequirements> getItemRequirementsFor(ItemStack paintbrush, EntityPlayerSP player, BlockPos pos) {
		if (!pos.equals(lastPosCached) || paintbrush != lastPaintbrushCached) {
			itemRequirementsCache = PaintbrushItem.getItemRequirements(paintbrush, player, player.world, pos);
			lastPosCached = pos;
			lastPaintbrushCached = paintbrush;
		}
		return itemRequirementsCache;
	}

	@SubscribeEvent
	public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen != null) {
			// what's in the inventory may change meanwhile
			lastPosCached = null;
			lastPaintbrushCached = null;
			return;
		}
		EntityPlayerSP player = mc.player;
		RayTraceResult hit = mc.objectMouseOver;
		if (player == null || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
			return;
		}
		BlockPos pos = hit.getBlockPos();
		Block block = player.world.getBlockState(pos).getBlock();
		if (!(block instanceof StorageBlockBase) && !(block instanceof ControllerBlock)) {
			return;
		}
		ItemStack paintbrush = player.getHeldItemMainhand().getItem() == ModItems.PAINTBRUSH ? player.getHeldItemMainhand()
				: player.getHeldItemOffhand().getItem() == ModItems.PAINTBRUSH ? player.getHeldItemOffhand() : ItemStack.EMPTY;
		if (paintbrush.isEmpty()) {
			return;
		}
		getItemRequirementsFor(paintbrush, player, pos).ifPresent(itemRequirements -> {
			if (itemRequirements.getItemsMissing().isEmpty()) {
				return;
			}
			ScaledResolution resolution = event.getResolution();
			FontRenderer font = mc.fontRenderer;
			String missingItems = I18n.format(ModItems.PAINTBRUSH.getTranslationKey() + ".overlay.missing_items");
			int textWidth = font.getStringWidth(missingItems);
			int x = (resolution.getScaledWidth() - textWidth) / 2;
			int y = resolution.getScaledHeight() - 75 - 10;
			Gui.drawRect(x - 1, y - 2, x + textWidth + 2, y + font.FONT_HEIGHT, 0x80_000000);
			font.drawStringWithShadow(missingItems, x + 1, y, 0xFFFFFF);
			x = (resolution.getScaledWidth() - itemRequirements.getItemsMissing().size() * 18) / 2;
			RenderHelper.enableGUIStandardItemLighting();
			for (ItemStack missingItem : itemRequirements.getItemsMissing()) {
				mc.getRenderItem().renderItemAndEffectIntoGUI(missingItem, x, y + 10);
				mc.getRenderItem().renderItemOverlays(font, missingItem, x, y + 10);
				x += 18;
			}
			RenderHelper.disableStandardItemLighting();
			GlStateManager.color(1, 1, 1, 1);
		});
	}
}
