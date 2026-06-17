package g_vael.cmsall.neoforge.client;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import g_vael.cmsall.client.CmsAllClient;
import g_vael.cmsall.client.CmsAllConfigScreen;

/**
 * NeoForge client-only setup. Isolated in its own class (loaded only inside the {@code Dist.CLIENT} branch) so the
 * dedicated server never touches NeoForge client classes.
 *
 * <p>Registers the "Config" button in the Mods list (main menu). It opens the CLIENT preferences only — server
 * settings are per-world and aren't available from the title screen, so they're not shown there.
 */
public final class CmsAllNeoForgeClient {

    private CmsAllNeoForgeClient() {
    }

    public static void init() {
        CmsAllClient.init();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (modContainer, parent) -> new CmsAllConfigScreen(parent, true));
    }
}
