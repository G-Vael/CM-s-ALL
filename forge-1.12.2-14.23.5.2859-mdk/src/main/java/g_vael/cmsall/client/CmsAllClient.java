package g_vael.cmsall.client;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import g_vael.cmsall.config.ClientConfigView;

/** Client-only setup: the rebindable "toggle chain" keybind, the pause-screen button and re-applying saved client prefs on join. */
public final class CmsAllClient {

    public static final KeyBinding TOGGLE_KEY =
            new KeyBinding("key.cmsall.toggle", Keyboard.KEY_NONE, "key.categories.cmsall");

    static boolean applyPending;

    private CmsAllClient() {
    }

    public static void preInit() {
        ClientRegistry.registerKeyBinding(TOGGLE_KEY);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        ClientConfigView.setSyncListener(new Runnable() {
            @Override
            public void run() {
                onServerSync();
            }
        });
    }

    /** On the first server config sync after joining, push our saved client prefs to the server. */
    static void onServerSync() {
        if (!applyPending) {
            return;
        }
        applyPending = false;
        ClientConfig cfg = ClientConfig.get();
        if (!cfg.mode.equalsIgnoreCase(ClientConfigView.get().defaultMode)) {
            runCommand("cmsall mode " + cfg.mode);
        }
        if (cfg.replant) {
            runCommand("cmsall replant true");
        }
        if (!cfg.protect) {
            runCommand("cmsall protect false");
        }
    }

    public static void runCommand(String command) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            mc.player.sendChatMessage("/" + command);
        }
    }
}
