package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.ColorButton;
import dev.feldsher.deconstructedcore.client.gui.controls.ColorPicker;
import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedstorage.DeconstructedStorage;
import dev.feldsher.deconstructedstorage.block.DecorationTableBlockEntity;
import dev.feldsher.deconstructedstorage.block.MaterialBlock;
import dev.feldsher.deconstructedstorage.common.gui.DecorationTableMenu;
import dev.feldsher.deconstructedstorage.util.DecorationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * A decoration table's screen: the material slots, each with an icon of the part it's for and an arrow between it and
 * the slot it inherits from; the dye slots and the two colors to pick; the decorated item, turning, which hovering a
 * slot turns to show its part, and which can be dragged around; the result, whose tooltip tells what it takes.
 */
public class DecorationTableScreen extends GuiContainer {
	public static final ResourceLocation GUI_BACKGROUND = DeconstructedStorage.getRL("textures/gui/decoration_table.png");
	public static final ResourceLocation GUI_DECORATION_TABLE_ELEMENTS = DeconstructedStorage.getRL("textures/gui/decoration_table_elements.png");
	private static final Dimension SQUARE_64 = new Dimension(64, 64);
	private static final Dimension SQUARE_8 = new Dimension(8, 8);
	private static final TextureBlitData TOP_INNER_TRIM_HIGHLIGHT = element(0, 0, Dimension.SQUARE_16);
	private static final TextureBlitData TOP_TRIM_HIGHLIGHT = element(0, 16, Dimension.SQUARE_16);
	private static final TextureBlitData SIDE_TRIM_HIGHLIGHT = element(0, 32, Dimension.SQUARE_16);
	private static final TextureBlitData BOTTOM_TRIM_HIGHLIGHT = element(0, 48, Dimension.SQUARE_16);
	private static final TextureBlitData TOP_CORE_HIGHLIGHT = element(16, 16, Dimension.SQUARE_16);
	private static final TextureBlitData SIDE_CORE_HIGHLIGHT = element(16, 32, Dimension.SQUARE_16);
	private static final TextureBlitData BOTTOM_CORE_HIGHLIGHT = element(16, 48, Dimension.SQUARE_16);
	private static final TextureBlitData ACCENT_TINT_HIGHLIGHT = element(32, 48, Dimension.SQUARE_16);
	private static final TextureBlitData MAIN_TINT_HIGHLIGHT = element(48, 48, Dimension.SQUARE_16);
	private static final TextureBlitData STORAGE_INFO = element(32, 16, Dimension.SQUARE_16);

	private static final ButtonDefinition.Toggle<Boolean> VERTICAL_INHERITANCE_ARROW = inheritanceArrow(new UV(56, 0), new UV(48, 0), new UV(32, 0), new UV(40, 0));
	private static final ButtonDefinition.Toggle<Boolean> HORIZONTAL_INHERITANCE_ARROW = inheritanceArrow(new UV(56, 8), new UV(48, 8), new UV(32, 8), new UV(40, 8));

	/** Which way the preview turns as each slot is hovered: to the part the slot is for. */
	private static final Map<Integer, int[]> SLOT_PREVIEW_ROTATIONS = new HashMap<>();

	static {
		SLOT_PREVIEW_ROTATIONS.put(0, new int[] {90, 180});
		SLOT_PREVIEW_ROTATIONS.put(1, new int[] {90, 180});
		SLOT_PREVIEW_ROTATIONS.put(4, new int[] {90, 180});
		SLOT_PREVIEW_ROTATIONS.put(2, new int[] {0, 180});
		SLOT_PREVIEW_ROTATIONS.put(5, new int[] {0, 180});
		SLOT_PREVIEW_ROTATIONS.put(3, new int[] {-90, 180});
		SLOT_PREVIEW_ROTATIONS.put(6, new int[] {-90, 180});
	}

	private final DecorationTableMenu menu;
	private final List<WidgetBase> widgets = new ArrayList<>();
	@Nullable
	private BlockPreview blockPreview;
	private long lastRotationSetTime = 0;
	@Nullable
	private ColorPicker colorPicker;
	private final List<String> resultPartsNeededTooltip = new ArrayList<>();

	public DecorationTableScreen(DecorationTableMenu menu) {
		super(menu);
		this.menu = menu;
		xSize = 250;
		ySize = 226;
		menu.setSlotChangedListener(this::updatePreviewStacks);
	}

	private static TextureBlitData element(int u, int v, Dimension dimension) {
		return new TextureBlitData(GUI_DECORATION_TABLE_ELEMENTS, SQUARE_64, new UV(u, v), dimension);
	}

	private static ButtonDefinition.Toggle<Boolean> inheritanceArrow(UV background, UV hoveredBackground, UV on, UV off) {
		Map<Boolean, ToggleButton.StateData> states = new HashMap<>();
		states.put(true, new ToggleButton.StateData(element(on.u(), on.v(), SQUARE_8),
				new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_on"))));
		states.put(false, new ToggleButton.StateData(element(off.u(), off.v(), SQUARE_8),
				new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translButton("decoration_inheritance_off"))));
		return new ButtonDefinition.Toggle<>(SQUARE_8, element(background.u(), background.v(), SQUARE_8), states, element(hoveredBackground.u(), hoveredBackground.v(), SQUARE_8));
	}

	private void updatePreviewStacks() {
		if (blockPreview != null) {
			blockPreview.setPreviewStacks(menu.getDecoratedPreviewStacks());
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		widgets.clear();
		Slot lastDyeSlot = menu.getSlot(DecorationTableMenu.FIRST_DYE_SLOT + 2);
		Slot resultSlot = menu.getResultSlot();
		blockPreview = new BlockPreview(new Position(guiLeft + lastDyeSlot.xPos + 16 + 1 + 8 + 1, guiTop + lastDyeSlot.yPos),
				new Dimension(80, resultSlot.yPos - lastDyeSlot.yPos + 16 + 4));
		updatePreviewStacks();
		widgets.add(blockPreview);

		addInheritanceArrow(DecorationTableBlockEntity.TOP_TRIM_SLOT, 4, -11, VERTICAL_INHERITANCE_ARROW);
		addInheritanceArrow(DecorationTableBlockEntity.SIDE_TRIM_SLOT, 4, -11, VERTICAL_INHERITANCE_ARROW);
		addInheritanceArrow(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT, 4, -11, VERTICAL_INHERITANCE_ARROW);
		addInheritanceArrow(DecorationTableBlockEntity.TOP_CORE_SLOT, -11, 4, HORIZONTAL_INHERITANCE_ARROW);
		addInheritanceArrow(DecorationTableBlockEntity.SIDE_CORE_SLOT, 4, -11, VERTICAL_INHERITANCE_ARROW);
		addInheritanceArrow(DecorationTableBlockEntity.BOTTOM_CORE_SLOT, 4, -11, VERTICAL_INHERITANCE_ARROW);

		addPartHint(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT, TOP_INNER_TRIM_HIGHLIGHT, "top_inner_trim");
		addPartHint(DecorationTableBlockEntity.TOP_TRIM_SLOT, TOP_TRIM_HIGHLIGHT, "top_trim");
		addPartHint(DecorationTableBlockEntity.SIDE_TRIM_SLOT, SIDE_TRIM_HIGHLIGHT, "side_trim");
		addPartHint(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT, BOTTOM_TRIM_HIGHLIGHT, "bottom_trim");
		addPartHint(DecorationTableBlockEntity.TOP_CORE_SLOT, TOP_CORE_HIGHLIGHT, "top");
		addPartHint(DecorationTableBlockEntity.SIDE_CORE_SLOT, SIDE_CORE_HIGHLIGHT, "side");
		addPartHint(DecorationTableBlockEntity.BOTTOM_CORE_SLOT, BOTTOM_CORE_HIGHLIGHT, "bottom");

		Slot slot = menu.getSlot(DecorationTableBlockEntity.TOP_INNER_TRIM_SLOT);
		widgets.add(new PartStorageInfo(new Position(guiLeft + slot.xPos + 57, guiTop + slot.yPos), menu::getPartsStored));
		addDyeElements();

		if (colorPicker != null) {
			colorPicker.setPosition(getColorPickerPosition());
		}
	}

	private Position getColorPickerPosition() {
		return new Position(guiLeft + (xSize - ColorPicker.DIMENSIONS.width()) / 2, guiTop + (ySize - ColorPicker.DIMENSIONS.height()) / 2);
	}

	private void addDyeElements() {
		Slot greenDyeSlot = menu.getSlot(DecorationTableMenu.FIRST_DYE_SLOT + 1);
		Slot topTrimSlot = menu.getSlot(DecorationTableBlockEntity.TOP_TRIM_SLOT);
		Slot sideTrimSlot = menu.getSlot(DecorationTableBlockEntity.SIDE_TRIM_SLOT);
		TextComponentTranslation pickColor = new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translButton("pick_color"));
		ColorButton mainColorButton = new ColorButton(new Position(guiLeft + greenDyeSlot.xPos - 1, guiTop + topTrimSlot.yPos - 1), new Dimension(18, 18),
				menu::getMainColor, button -> openColorPicker(menu.getMainColor(), menu::setMainColor), pickColor);
		widgets.add(mainColorButton);
		ColorButton accentColorButton = new ColorButton(new Position(guiLeft + greenDyeSlot.xPos - 1, guiTop + sideTrimSlot.yPos - 1), new Dimension(18, 18),
				menu::getAccentColor, button -> openColorPicker(menu.getAccentColor(), menu::setAccentColor), pickColor);
		widgets.add(accentColorButton);
		widgets.add(new PartIcon(new Position(mainColorButton.getX() + mainColorButton.getWidth() + 1, mainColorButton.getY() + 1), MAIN_TINT_HIGHLIGHT,
				StorageTranslationHelper.INSTANCE.translGui("tint.main")));
		widgets.add(new PartIcon(new Position(accentColorButton.getX() + accentColorButton.getWidth() + 1, accentColorButton.getY() + 1), ACCENT_TINT_HIGHLIGHT,
				StorageTranslationHelper.INSTANCE.translGui("tint.accent")));
	}

	private void openColorPicker(int color, IntConsumer colorSetter) {
		colorPicker = new ColorPicker(getColorPickerPosition(), color, c -> {
			colorSetter.accept(c);
			closeColorPicker();
		});
		if (blockPreview != null) {
			blockPreview.setVisible(false);
		}
	}

	private void closeColorPicker() {
		colorPicker = null;
		if (blockPreview != null) {
			blockPreview.setVisible(true);
		}
		updatePreviewStacks();
	}

	private void addInheritanceArrow(int slotIndex, int xOffset, int yOffset, ButtonDefinition.Toggle<Boolean> arrowDefinition) {
		Slot slot = menu.getSlot(slotIndex);
		widgets.add(new ToggleButton<Boolean>(new Position(guiLeft + slot.xPos + xOffset, guiTop + slot.yPos + yOffset), arrowDefinition, button -> {
			resultPartsNeededTooltip.clear();
			menu.setSlotMaterialInheritance(slotIndex, !menu.isSlotMaterialInherited(slotIndex));
			updatePreviewStacks();
		}, () -> menu.isSlotMaterialInherited(slotIndex)) {
			@Override
			public void render(int mouseX, int mouseY, float partialTicks) {
				super.render(mouseX, mouseY, partialTicks);
				int[] rotations = SLOT_PREVIEW_ROTATIONS.get(slotIndex);
				if (isMouseOver(mouseX, mouseY) && rotations != null) {
					setPreviewRotations(rotations[0], rotations[1]);
				}
			}
		});
	}

	private void addPartHint(int slotIndex, TextureBlitData texture, String barrelPart) {
		Slot slot = menu.getSlot(slotIndex);
		widgets.add(new PartIcon(new Position(guiLeft + slot.xPos + 18, guiTop + slot.yPos), texture, StorageTranslationHelper.INSTANCE.translGui("barrel_part." + barrelPart)));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		GuiHelper.blit(GUI_BACKGROUND, guiLeft, guiTop, 0, 0, xSize, ySize);
		renderDyeSlotsOverlays();
		renderInheritedItems();
		for (WidgetBase widget : widgets) {
			widget.render(mouseX, mouseY, partialTicks);
		}
	}

	private void renderDyeSlotsOverlays() {
		renderSlotOverlay(menu.getSlot(DecorationTableMenu.FIRST_DYE_SLOT), 0x33_FF0000);
		renderSlotOverlay(menu.getSlot(DecorationTableMenu.FIRST_DYE_SLOT + 1), 0x33_00FF00);
		renderSlotOverlay(menu.getSlot(DecorationTableMenu.FIRST_DYE_SLOT + 2), 0x33_0000FF);
	}

	private void renderSlotOverlay(Slot slot, int slotColor) {
		GlStateManager.enableBlend();
		GlStateManager.colorMask(true, true, true, false);
		drawRect(guiLeft + slot.xPos, guiTop + slot.yPos, guiLeft + slot.xPos + 16, guiTop + slot.yPos + 16, slotColor);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.disableBlend();
		GlStateManager.color(1, 1, 1, 1);
	}

	/** An empty material slot that inherits shows the block it takes, under a translucent cover. */
	private void renderInheritedItems() {
		for (int slotIndex = 0; slotIndex < DecorationTableMenu.FIRST_DYE_SLOT; slotIndex++) {
			Slot slot = menu.getSlot(slotIndex);
			if (slot.getHasStack() || !menu.isSlotMaterialInherited(slotIndex)) {
				continue;
			}
			ItemStack inheritedItem = menu.getInheritedItem(slotIndex);
			if (inheritedItem.isEmpty()) {
				continue;
			}
			RenderHelper.enableGUIStandardItemLighting();
			itemRender.renderItemAndEffectIntoGUI(inheritedItem, guiLeft + slot.xPos, guiTop + slot.yPos);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableDepth();
			GlStateManager.enableBlend();
			GlStateManager.color(1, 1, 1, 1);
			GuiHelper.blit(GuiHelper.GUI_CONTROLS, guiLeft + slot.xPos, guiTop + slot.yPos, 77, 0, 16, 16);
			GlStateManager.disableBlend();
			GlStateManager.enableDepth();
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRenderer.drawString(I18n.format(dev.feldsher.deconstructedstorage.init.ModBlocks.DECORATION_TABLE.getTranslationKey() + ".name"), 8, 6, 0x404040);
		fontRenderer.drawString(I18n.format("container.inventory"), 45, menu.getSlot(DecorationTableBlockEntity.BOTTOM_TRIM_SLOT).yPos + 18 + 2, 0x404040);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		updatePreviewRotation(mouseX, mouseY);
		drawDefaultBackground();
		boolean pickingColor = colorPicker != null;
		// the slots' items stay under the color picker
		super.drawScreen(pickingColor ? -1 : mouseX, pickingColor ? -1 : mouseY, partialTicks);
		if (colorPicker != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(0, 0, 500);
			drawGradientRect(0, 0, width, height, 0xC0_101010, 0xD0_101010);
			colorPicker.render(mouseX, mouseY, partialTicks);
			colorPicker.renderTooltip(this, mouseX, mouseY);
			GlStateManager.popMatrix();
			return;
		}
		for (WidgetBase widget : widgets) {
			widget.renderTooltip(this, mouseX, mouseY);
		}
		renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void renderToolTip(ItemStack stack, int x, int y) {
		Slot hovered = getSlotUnderMouse();
		if (hovered != menu.getResultSlot() || !DecorationTableBlockEntity.consumesIngredients(stack)) {
			resultPartsNeededTooltip.clear();
			super.renderToolTip(stack, x, y);
			return;
		}
		List<String> tooltip = getItemToolTip(stack);
		tooltip.addAll(getResultPartsNeededTooltip());
		net.minecraftforge.fml.client.config.GuiUtils.preItemToolTip(stack);
		drawHoveringText(tooltip, x, y, fontRenderer);
		net.minecraftforge.fml.client.config.GuiUtils.postItemToolTip();
	}

	private List<String> getResultPartsNeededTooltip() {
		if (!resultPartsNeededTooltip.isEmpty()) {
			return resultPartsNeededTooltip;
		}
		addPartCountInfo(menu.getPartsNeeded(), resultPartsNeededTooltip,
				key -> menu.getMissingDyes().contains(key) ? TextFormatting.RED : TextFormatting.DARK_GRAY);
		return resultPartsNeededTooltip;
	}

	/** "18/24 (75%) of Red Dye": how much of each block or dye, by name. */
	private static void addPartCountInfo(Map<String, Integer> partCounts, List<String> tooltip, Function<String, TextFormatting> getPartFormatting) {
		Map<ItemStack, String> itemKeys = new LinkedHashMap<>();
		Map<String, Integer> counts = new HashMap<>(partCounts);
		partCounts.keySet().forEach(key -> getPartStack(key).ifPresent(stack -> itemKeys.put(stack, key)));
		itemKeys.entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().getDisplayName())).forEach(entry -> {
			int count = counts.get(entry.getValue());
			String partCountText = count + "/" + DecorationHelper.BLOCK_TOTAL_PARTS + " (" + String.format("%.0f%%", (float) count / DecorationHelper.BLOCK_TOTAL_PARTS * 100)
					+ ") of " + entry.getKey().getDisplayName();
			tooltip.add(getPartFormatting.apply(entry.getValue()) + partCountText);
		});
	}

	/** The stack a part key is of: a dye's first item by the ore dictionary, a material's block. */
	@SuppressWarnings("deprecation")
	private static java.util.Optional<ItemStack> getPartStack(String key) {
		if (key.startsWith("dye")) {
			List<ItemStack> ores = OreDictionary.getOres(key);
			return ores.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(ores.get(0));
		}
		MaterialBlock block = MaterialBlock.parse(key);
		if (block == null) {
			return java.util.Optional.empty();
		}
		return block.getState().map(state -> {
			Item item = Item.getItemFromBlock(state.getBlock());
			return new ItemStack(item, 1, state.getBlock().damageDropped(state));
		});
	}

	private void updatePreviewRotation(int mouseX, int mouseY) {
		if (blockPreview == null) {
			return;
		}
		SLOT_PREVIEW_ROTATIONS.forEach((slotIndex, rotation) -> {
			Slot slot = menu.getSlot(slotIndex);
			int slotLeft = guiLeft + slot.xPos;
			int slotTop = guiTop + slot.yPos;
			if (slotLeft <= mouseX && mouseX < slotLeft + 16 + 18 && slotTop <= mouseY && mouseY < slotTop + 16) {
				setPreviewRotations(rotation[0], rotation[1]);
			}
		});
		if (lastRotationSetTime != 0 && System.currentTimeMillis() - lastRotationSetTime > 1000) {
			blockPreview.resetToDefaultRotation();
			lastRotationSetTime = 0;
		}
	}

	private void setPreviewRotations(int xAxisRotation, int yAxisRotation) {
		if (blockPreview != null) {
			blockPreview.setTargetRotations(xAxisRotation, yAxisRotation);
			lastRotationSetTime = System.currentTimeMillis();
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (colorPicker != null) {
			colorPicker.onScreenClicked(mouseX, mouseY, mouseButton);
			colorPicker.mouseClicked(mouseX, mouseY, mouseButton);
			return;
		}
		for (WidgetBase widget : new ArrayList<>(widgets)) {
			if (widget.isMouseOver(mouseX, mouseY) && widget.mouseClicked(mouseX, mouseY, mouseButton)) {
				return;
			}
		}
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (colorPicker != null) {
			colorPicker.mouseDragged(mouseX, mouseY, clickedMouseButton);
			return;
		}
		if (blockPreview != null && blockPreview.isMouseOver(mouseX, mouseY) && blockPreview.mouseDragged(mouseX, mouseY, clickedMouseButton)) {
			lastRotationSetTime = System.currentTimeMillis() + 100_000;
			return;
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (colorPicker != null) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				closeColorPicker();
			} else {
				colorPicker.keyTyped(typedChar, keyCode);
			}
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (blockPreview != null) {
			blockPreview.trackDrag(Mouse.getEventX() * width / mc.displayWidth, height - Mouse.getEventY() * height / mc.displayHeight - 1);
		}
	}

	private static class PartIcon extends WidgetBase {
		private final TextureBlitData texture;
		private final String tooltipKey;

		PartIcon(Position position, TextureBlitData texture, String tooltipKey) {
			super(position, new Dimension(texture.getWidth(), texture.getHeight()));
			this.texture = texture;
			this.tooltipKey = tooltipKey;
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			GlStateManager.color(1, 1, 1, 1);
			GuiHelper.blit(x, y, texture);
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			//noop
		}

		@Override
		public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
			if (isMouseOver(mouseX, mouseY)) {
				screen.drawHoveringText(Collections.singletonList(I18n.format(tooltipKey)), mouseX, mouseY);
			}
		}
	}

	private static class PartStorageInfo extends WidgetBase {
		private final Supplier<Map<String, Integer>> getPartsStored;

		PartStorageInfo(Position position, Supplier<Map<String, Integer>> getPartsStored) {
			super(position, Dimension.SQUARE_16);
			this.getPartsStored = getPartsStored;
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			if (!getPartsStored.get().isEmpty()) {
				GlStateManager.color(1, 1, 1, 1);
				GuiHelper.blit(x, y, STORAGE_INFO);
			}
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			//noop
		}

		@Override
		public void renderTooltip(GuiScreen screen, int mouseX, int mouseY) {
			if (getPartsStored.get().isEmpty() || !isMouseOver(mouseX, mouseY)) {
				return;
			}
			List<String> tooltip = new ArrayList<>();
			tooltip.add(I18n.format(StorageTranslationHelper.INSTANCE.translGuiTooltip("parts_stored")));
			addPartCountInfo(getPartsStored.get(), tooltip, key -> TextFormatting.GRAY);
			screen.drawHoveringText(tooltip, mouseX, mouseY);
		}
	}

	private static class StackButton extends ButtonBase {
		private static final TextureBlitData BUTTON_HOVER = new TextureBlitData(GuiHelper.GUI_CONTROLS, Dimension.SQUARE_256, new UV(63, 42), Dimension.SQUARE_18);
		private final Supplier<ItemStack> stackSupplier;

		StackButton(Position position, IntConsumer onClick, Supplier<ItemStack> stackSupplier) {
			super(position, Dimension.SQUARE_18, onClick);
			this.stackSupplier = stackSupplier;
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			//noop
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			ItemStack stack = stackSupplier.get();
			if (stack.isEmpty()) {
				return;
			}
			GuiHelper.renderItemInGUI(stack, x + 1, y + 1);
			if (isMouseOver(mouseX, mouseY)) {
				GlStateManager.color(1, 1, 1, 1);
				GuiHelper.blit(x, y, BUTTON_HOVER);
			}
		}
	}

	/** The decorated item, big and turning to where it's set to, eased; dragged, it turns with the mouse. */
	private static class BlockPreview extends CompositeWidgetBase<WidgetBase> {
		private final List<ItemStack> previewStacks = new ArrayList<>();
		private float xAxisRotation = 30;
		private float yAxisRotation = 45;
		private float fromXAxisRotation = xAxisRotation;
		private float fromYAxisRotation = yAxisRotation;
		private float targetXAxisRotation = xAxisRotation;
		private float targetYAxisRotation = yAxisRotation;
		private long lastTargetSetTime = 0;
		private int selectedPreview = 0;
		private final List<StackButton> previewStackButtons = new ArrayList<>();
		private int lastDragX = Integer.MIN_VALUE;
		private int lastDragY = Integer.MIN_VALUE;

		BlockPreview(Position position, Dimension dimension) {
			super(position, dimension);
		}

		public void setPreviewStacks(List<ItemStack> previewStacks) {
			this.previewStacks.clear();
			this.previewStacks.addAll(previewStacks);
			selectedPreview = 0;
			updatePreviewStackButtons();
			resetToDefaultRotation();
		}

		private void updatePreviewStackButtons() {
			children.removeAll(previewStackButtons);
			previewStackButtons.clear();
			if (previewStacks.size() < 2) {
				return;
			}
			int buttonsX = x + (getWidth() - previewStacks.size() * (18 + 2)) / 2;
			for (int i = 0; i < previewStacks.size(); i++) {
				ItemStack stack = previewStacks.get(i);
				int index = i;
				previewStackButtons.add(new StackButton(new Position(buttonsX + i * 20, y + getHeight() - 19), button -> {
					selectedPreview = index;
					resetToDefaultRotation();
				}, () -> stack));
			}
			previewStackButtons.forEach(this::addChild);
		}

		/** As the item is shown in the inventory. */
		public void resetToDefaultRotation() {
			if (previewStacks.isEmpty() || previewStacks.get(selectedPreview).isEmpty()) {
				return;
			}
			ItemTransformVec3f guiTransform = minecraft.getRenderItem().getItemModelWithOverrides(previewStacks.get(selectedPreview), null, null).getItemCameraTransforms()
					.getTransform(ItemCameraTransforms.TransformType.GUI);
			setTargetRotations((int) guiTransform.rotation.x, (int) guiTransform.rotation.y);
		}

		public void setTargetRotations(int xAxisRotation, int yAxisRotation) {
			if (targetXAxisRotation == xAxisRotation && targetYAxisRotation == yAxisRotation) {
				return;
			}
			fromXAxisRotation = this.xAxisRotation;
			fromYAxisRotation = this.yAxisRotation;
			targetXAxisRotation = xAxisRotation;
			targetYAxisRotation = yAxisRotation;
			lastTargetSetTime = System.currentTimeMillis();
		}

		@Override
		protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
			drawRect(x, y, x + getWidth(), y + getHeight(), 0xFF_000000);
		}

		@Override
		protected void renderWidget(int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(mouseX, mouseY, partialTicks);
			if (previewStacks.isEmpty() || previewStacks.get(selectedPreview).isEmpty()) {
				return;
			}
			ItemStack previewStack = previewStacks.get(selectedPreview);
			updateRotations();

			IBakedModel model = minecraft.getRenderItem().getItemModelWithOverrides(previewStack, null, null);
			GlStateManager.pushMatrix();
			float yCenter = (getHeight() - (previewStackButtons.isEmpty() ? 0 : 20)) / 2F;
			GlStateManager.translate(x + getWidth() / 2F, y + yCenter, 150);
			GlStateManager.rotate(-xAxisRotation, 1, 0, 0);
			GlStateManager.rotate(yAxisRotation, 0, 1, 0);
			int scale = 48;
			GlStateManager.scale(scale, -scale, scale);
			minecraft.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GlStateManager.enableRescaleNormal();
			GlStateManager.enableBlend();
			GlStateManager.color(1, 1, 1, 1);
			RenderHelper.enableStandardItemLighting();
			minecraft.getRenderItem().renderItem(previewStack, model);
			RenderHelper.disableStandardItemLighting();
			GlStateManager.disableBlend();
			GlStateManager.disableRescaleNormal();
			GlStateManager.popMatrix();
		}

		private void updateRotations() {
			long elapsed = System.currentTimeMillis() - lastTargetSetTime;
			if (elapsed <= 1000) {
				float ratio = easeInOutCubic(elapsed / 1000F);
				xAxisRotation = fromXAxisRotation + (targetXAxisRotation - fromXAxisRotation) * ratio;
				yAxisRotation = fromYAxisRotation + (targetYAxisRotation - fromYAxisRotation) * ratio;
			} else {
				xAxisRotation = targetXAxisRotation;
				yAxisRotation = targetYAxisRotation;
			}
		}

		private static float easeInOutCubic(float x) {
			return x < 0.5F ? 4 * x * x * x : 1 - (float) Math.pow(-2 * x + 2, 3) / 2;
		}

		/** Where the mouse is now, for the next drag to turn by how far it moved. */
		void trackDrag(int mouseX, int mouseY) {
			if (!Mouse.isButtonDown(0)) {
				lastDragX = Integer.MIN_VALUE;
				lastDragY = Integer.MIN_VALUE;
			}
		}

		@Override
		public boolean mouseDragged(double mouseX, double mouseY, int button) {
			if (!previewStackButtons.isEmpty() && mouseY > y + getHeight() - 20) {
				return super.mouseDragged(mouseX, mouseY, button);
			}
			int dragX = lastDragX == Integer.MIN_VALUE ? 0 : (int) mouseX - lastDragX;
			int dragY = lastDragY == Integer.MIN_VALUE ? 0 : (int) mouseY - lastDragY;
			lastDragX = (int) mouseX;
			lastDragY = (int) mouseY;
			yAxisRotation = (yAxisRotation + 2 * dragX) % 360;
			xAxisRotation = (xAxisRotation + 2 * dragY) % 360;
			targetXAxisRotation = xAxisRotation;
			targetYAxisRotation = yAxisRotation;
			return true;
		}
	}
}
