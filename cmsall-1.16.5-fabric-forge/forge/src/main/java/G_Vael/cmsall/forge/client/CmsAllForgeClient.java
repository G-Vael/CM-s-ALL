package G_Vael.cmsall.forge.client;

import java.util.function.BiFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

import G_Vael.cmsall.client.CmsAllClient;
import G_Vael.cmsall.client.CmsAllConfigScreen;

/**
 * Forge client-only setup (1.16.5). Isolated in its own class (loaded only inside the {@code Dist.CLIENT} branch) so
 * the dedicated server never touches Forge client classes.
 *
 * <p>Registers the "Config" button in the Mods list (main menu). It opens the CLIENT preferences only — server
 * settings are per-world and aren't available from the title screen, so they're not shown there.
 */
public final class CmsAllForgeClient {

    private CmsAllForgeClient() {
    }

    public static void init() {
        CmsAllClient.init();
        BiFunction<Minecraft, Screen, Screen> factory = (minecraft, screen) -> new CmsAllConfigScreen(screen, true);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> factory);
    }
}
