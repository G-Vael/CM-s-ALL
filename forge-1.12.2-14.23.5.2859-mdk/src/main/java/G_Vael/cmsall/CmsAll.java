package G_Vael.cmsall;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;

import G_Vael.cmsall.command.CmsAllCommand;
import G_Vael.cmsall.config.ConfigManager;
import G_Vael.cmsall.core.PlacedBlocksTracker;
import G_Vael.cmsall.net.CmsAllNetwork;
import G_Vael.cmsall.proxy.CommonProxy;

/** CM'sALL — Forge 1.12.2 entry point. */
@Mod(modid = CmsAll.MOD_ID, name = "CM'sALL", version = "1.0.0", acceptedMinecraftVersions = "[1.12.2]",
        guiFactory = "G_Vael.cmsall.client.CmsAllGuiFactory")
public class CmsAll {

    public static final String MOD_ID = "cmsall";
    public static final Logger LOGGER = LogManager.getLogger("CM'sALL");

    @SidedProxy(clientSide = "G_Vael.cmsall.proxy.ClientProxy", serverSide = "G_Vael.cmsall.proxy.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("[CM'sALL] preInit");
        ConfigManager.setConfigDir(event.getModConfigurationDirectory());
        ConfigManager.load();
        CmsAllNetwork.register();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        proxy.init();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CmsAllCommand());
        ConfigManager.apply(); // re-apply once registries are fully populated (modded ids resolve)
        PlacedBlocksTracker.trim(event.getServer());
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ForgeEvents.onServerStopped(event); // clear transient queues
    }
}
