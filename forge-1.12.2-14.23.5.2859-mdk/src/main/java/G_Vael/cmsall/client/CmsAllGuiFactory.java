package G_Vael.cmsall.client;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

/**
 * Old-Forge (1.12.2) Mods-list "Config" button. Loaded by the client's mod-list GUI only, so the dedicated server
 * never touches it. Opens the CLIENT preferences only — server settings are per-world and aren't available from the
 * title screen, so they're not shown there.
 */
public class CmsAllGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new CmsAllConfigGui(parentScreen, true);
    }

    @Override
    public Set<IModGuiFactory.RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }
}
