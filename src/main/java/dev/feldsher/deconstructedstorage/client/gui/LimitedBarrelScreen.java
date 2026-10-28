package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.LimitedBarrelBlockEntity;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

import java.util.function.IntFunction;

/**
 * A limited barrel's screen: its one to four slots laid out as on its front, with how full each is beside it. It has
 * no sort buttons, there being nothing to sort.
 */
public class LimitedBarrelScreen extends StorageScreen {
	public static final ResourceLocation GUI_BACKGROUNDS = DeconstructedStorage.getRL("textures/gui/limited_barrels.png");
	private static final int BACKGROUND_WIDTH = 84;
	private static final int BACKGROUND_HEIGHT = 82;
	public static final int STORAGE_SLOTS_HEIGHT = 82;
	private static final int MIDDLE_OF_STORAGE_SLOTS = 18 + STORAGE_SLOTS_HEIGHT / 2;
	private static final int SMALL_BAR_HEIGHT = 28;
	private static final int LARGE_BAR_HEIGHT = 68;

	public LimitedBarrelScreen(StorageContainerMenu menu) {
		super(menu);
	}

	@Override
	protected int getStorageInventoryHeight(int displayableNumberOfRows) {
		return STORAGE_SLOTS_HEIGHT;
	}

	@Override
	protected void updateStorageSlotsPositions() {
		updateSlotPositions(getMenu(), getMenu().getNumberOfStorageInventorySlots(), xSize);
	}

	@Override
	protected void drawSlotBg(int x, int y, int visibleSlotsCount) {
		drawSlotBg(x, y, xSize, getMenu().getNumberOfStorageInventorySlots());
	}

	/** Its slots are all left out of sorting, which is no news to mark them with. */
	@Override
	protected boolean showsSlotColors() {
		return false;
	}

	@Override
	protected boolean shouldShowSortButtons() {
		return false;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		LimitedBarrelBlockEntity barrel = (LimitedBarrelBlockEntity) getMenu().getStorageBlockEntity();
		GlStateManager.disableLighting();
		renderBars(fontRenderer, xSize, getMenu().getNumberOfStorageInventorySlots(), barrel::getSlotFillPercentage);
	}

	/** The slots' background: one of the four layouts, by how many slots there are. */
	public static void drawSlotBg(int x, int y, int xSize, int slotsNumber) {
		int u = (slotsNumber - 1) % 2 * BACKGROUND_WIDTH;
		int v = (slotsNumber - 1) / 2 * BACKGROUND_HEIGHT;
		GuiHelper.blit(GUI_BACKGROUNDS, x + xSize / 2 - BACKGROUND_WIDTH / 2 - 1, y + 17, u, v, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
	}

	/** Where the slots are: one in the middle, two above each other, one over two, or two by two. */
	public static void updateSlotPositions(Container menu, int slotNumber, int imageWidth) {
		int halfWidth = imageWidth / 2;
		int[][] positions;
		switch (slotNumber) {
			case 1:
				positions = new int[][] {{-9, -9}};
				break;
			case 2:
				positions = new int[][] {{-9, -29}, {-9, 11}};
				break;
			case 3:
				positions = new int[][] {{-9, -29}, {-29, 11}, {11, 11}};
				break;
			default:
				positions = new int[][] {{-29, -29}, {11, -29}, {-29, 11}, {11, 11}};
				break;
		}
		for (int slotIndex = 0; slotIndex < positions.length && slotIndex < slotNumber; slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			slot.xPos = halfWidth + positions[slotIndex][0];
			slot.yPos = MIDDLE_OF_STORAGE_SLOTS + positions[slotIndex][1];
		}
	}

	/** By each slot, a bar filled as far as the slot is, with the percentage next to it. */
	public static void renderBars(FontRenderer font, int imageWidth, int slotsNumber, IntFunction<Float> getSlotFillPercentage) {
		int right = imageWidth / 2 + 37;
		int left = imageWidth / 2 - 37 - 5;
		int top = 18 + 6;
		int bottom = 18 + 6 + 40;
		switch (slotsNumber) {
			case 1:
				renderBar(font, right, top, getSlotFillPercentage.apply(0), true, false);
				break;
			case 2:
				renderBar(font, right, top, getSlotFillPercentage.apply(0), false, false);
				renderBar(font, right, bottom, getSlotFillPercentage.apply(1), false, false);
				break;
			case 3:
				renderBar(font, right, top, getSlotFillPercentage.apply(0), false, false);
				renderBar(font, left, bottom, getSlotFillPercentage.apply(1), false, true);
				renderBar(font, right, bottom, getSlotFillPercentage.apply(2), false, false);
				break;
			case 4:
				renderBar(font, left, top, getSlotFillPercentage.apply(0), false, true);
				renderBar(font, right, top, getSlotFillPercentage.apply(1), false, false);
				renderBar(font, left, bottom, getSlotFillPercentage.apply(2), false, true);
				renderBar(font, right, bottom, getSlotFillPercentage.apply(3), false, false);
				break;
			default:
				break;
		}
	}

	private static void renderBar(FontRenderer font, int x, int y, float percentage, boolean large, boolean left) {
		int height = large ? LARGE_BAR_HEIGHT : SMALL_BAR_HEIGHT;
		int barHeight = (int) (height * percentage);
		int yOffset = height - barHeight;
		GlStateManager.color(1, 1, 1, 1);
		GuiHelper.blit(GUI_BACKGROUNDS, x, y + yOffset, large ? 168 : 171, yOffset, 3, barHeight);

		String text = (int) (percentage * 100) + "%";
		int percentageX = left ? x - 2 - font.getStringWidth(text) : x + 6;
		font.drawString(text, percentageX, y + height / 2 - 3, 0x2c2c2c);
	}
}
