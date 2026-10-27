package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.controls.ButtonDefinition;
import dev.feldsher.deconstructedcore.client.gui.controls.CompositeWidgetBase;
import dev.feldsher.deconstructedcore.client.gui.controls.ToggleButton;
import dev.feldsher.deconstructedcore.client.gui.controls.WidgetBase;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedstorage.common.gui.BlockSide;
import dev.feldsher.deconstructedstorage.common.gui.SideIOContainer;
import dev.feldsher.deconstructedstorage.upgrades.IOMode;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** The sides of the storage laid out as a cross around its front, each a button for what's done on it. */
public class SideIOControl extends CompositeWidgetBase<WidgetBase> {
	public SideIOControl(SideIOContainer container, Position position) {
		super(position, new Dimension(54, 54));

		addSideIOButton(container, new Position(x + 18, y), BlockSide.TOP);
		addSideIOButton(container, new Position(x, y + 18), BlockSide.LEFT);
		addSideIOButton(container, new Position(x + 18, y + 18), BlockSide.FRONT);
		addSideIOButton(container, new Position(x + 36, y + 18), BlockSide.RIGHT);
		addSideIOButton(container, new Position(x + 18, y + 36), BlockSide.BOTTOM);
		addSideIOButton(container, new Position(x + 36, y + 36), BlockSide.BACK);
	}

	private void addSideIOButton(SideIOContainer container, Position position, BlockSide side) {
		addChild(new SideIOToggleButton(position, StorageButtonDefinitions.IO_MODE, button -> container.toggleSideIO(side), () -> container.getSideIOMode(side),
				side, container::toDirection));
	}

	@Override
	protected void renderBg(Minecraft minecraft, int mouseX, int mouseY) {
		//noop
	}

	private static class SideIOToggleButton extends ToggleButton<IOMode> {
		private final BlockSide side;
		private final Function<BlockSide, EnumFacing> getDirection;

		public SideIOToggleButton(Position position, ButtonDefinition.Toggle<IOMode> buttonDefinition, IntConsumer onClick, Supplier<IOMode> getState,
				BlockSide side, Function<BlockSide, EnumFacing> getDirection) {
			super(position, buttonDefinition, onClick, getState);
			this.side = side;
			this.getDirection = getDirection;
		}

		@Override
		protected List<ITextComponent> getTooltip(StateData data) {
			List<ITextComponent> tooltip = new ArrayList<>(data.getTooltip());
			ITextComponent sideInfo = new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translUpgradeButton("io_mode_side_info"),
					new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translGui("block_side." + side.getSerializedName())),
					new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translGui("direction." + getDirection.apply(side).getName())));
			sideInfo.getStyle().setColor(TextFormatting.DARK_GRAY);
			tooltip.add(sideInfo);
			return tooltip;
		}
	}
}
