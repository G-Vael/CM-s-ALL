package G_Vael.cmsall.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import G_Vael.cmsall.client.CmsAllConfigScreen;

/**
 * Fabric Mods-list "Config" button, via the (optional) ModMenu API. Opens the CLIENT preferences only — server
 * settings are per-world and aren't available from the title screen, so they're not shown there.
 *
 * <p>Soft dependency: ModMenu is {@code modCompileOnly}, and this "modmenu" entrypoint is only ever queried by ModMenu
 * itself, so when ModMenu isn't installed this class is never loaded (no missing-class crash).
 */
public final class CmsAllModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CmsAllConfigScreen(parent, true);
    }
}
