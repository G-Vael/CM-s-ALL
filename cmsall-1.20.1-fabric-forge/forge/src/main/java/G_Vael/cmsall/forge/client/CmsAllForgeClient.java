package G_Vael.cmsall.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

import G_Vael.cmsall.client.CmsAllClient;
import G_Vael.cmsall.client.CmsAllConfigScreen;

/**
 * Forge client-only setup. Isolated in its own class (loaded only inside the {@code Dist.CLIENT} branch) so the
 * dedicated server never touches Forge client classes.
 *
 * <p>Registers the "Config" button in the Mods list (main menu). It opens the CLIENT preferences only — server
 * settings are per-world and aren't available from the title screen, so they're not shown there.
 */
public final class CmsAllForgeClient {

    private CmsAllForgeClient() {
    }

    public static void init() {
        CmsAllClient.init();
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new CmsAllConfigScreen(parent, true)));
    }
}
