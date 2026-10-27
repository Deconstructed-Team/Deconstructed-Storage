package dev.feldsher.deconstructedstorage.client.gui;

import dev.feldsher.deconstructedcore.client.gui.Tab;
import dev.feldsher.deconstructedcore.client.gui.controls.ImageButton;
import dev.feldsher.deconstructedcore.client.gui.utils.Dimension;
import dev.feldsher.deconstructedcore.client.gui.utils.GuiHelper;
import dev.feldsher.deconstructedcore.client.gui.utils.Position;
import dev.feldsher.deconstructedcore.client.gui.utils.TextureBlitData;
import dev.feldsher.deconstructedcore.client.gui.utils.UV;
import dev.feldsher.deconstructedstorage.network.OpenStorageInventoryMessage;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import net.minecraft.util.text.TextComponentTranslation;

public class BackToStorageTab extends Tab {
	private static final TextureBlitData ICON = new TextureBlitData(GuiHelper.ICONS, Dimension.SQUARE_256, new UV(64, 80), Dimension.SQUARE_16);

	protected BackToStorageTab(Position position) {
		super(position, new TextComponentTranslation(StorageTranslationHelper.INSTANCE.translGui("back_to_storage.tooltip")),
				onTabIconClicked -> new ImageButton(new Position(position.x() + 1, position.y() + 4), Dimension.SQUARE_16, ICON, onTabIconClicked));
	}

	@Override
	protected void onTabIconClicked(int button) {
		PacketHandler.INSTANCE.sendToServer(new OpenStorageInventoryMessage());
	}
}
