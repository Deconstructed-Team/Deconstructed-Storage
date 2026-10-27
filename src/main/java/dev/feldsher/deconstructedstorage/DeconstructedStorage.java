package dev.feldsher.deconstructedstorage;

import dev.feldsher.deconstructedcore.compat.bogosorter.BogoSorterCompat;
import dev.feldsher.deconstructedstorage.common.gui.GuiHandler;
import dev.feldsher.deconstructedstorage.crafting.OreDependentRecipes;
import dev.feldsher.deconstructedstorage.common.gui.StorageContainerMenu;
import dev.feldsher.deconstructedstorage.network.PacketHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = DeconstructedStorage.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION,
		dependencies = "required-after:deconstructedcore")
public class DeconstructedStorage {
	public static final String MOD_ID = Tags.MOD_ID;
	public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

	@Mod.Instance(MOD_ID)
	public static DeconstructedStorage instance;

	@SidedProxy(clientSide = "dev.feldsher.deconstructedstorage.client.ClientProxy", serverSide = "dev.feldsher.deconstructedstorage.CommonProxy")
	public static CommonProxy proxy;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		LOGGER.info("{} {} loading", Tags.MOD_NAME, Tags.VERSION);
		PacketHandler.init();
		proxy.preInit();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, new GuiHandler());
		BogoSorterCompat.registerStorageContainers(StorageContainerMenu.class);
		proxy.init();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		// Every mod has registered its ores by now
		OreDependentRecipes.removeRecipesWithoutOres();
	}

	public static ResourceLocation getRL(String regName) {
		return new ResourceLocation(MOD_ID, regName);
	}
}
