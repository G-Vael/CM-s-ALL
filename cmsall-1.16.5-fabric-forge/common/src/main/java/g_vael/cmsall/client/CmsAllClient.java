package g_vael.cmsall.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import me.shedaniel.architectury.event.events.GuiEvent;
import me.shedaniel.architectury.event.events.client.ClientPlayerEvent;
import me.shedaniel.architectury.event.events.client.ClientTickEvent;
import me.shedaniel.architectury.registry.KeyBindings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import g_vael.cmsall.config.ClientConfigView;

/** Client-only setup. */
public final class CmsAllClient {

    /** Rebindable in vanilla Controls; unbound by default so it never clashes. */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.cmsall.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.cmsall");

    /** Set on join; the first config sync afterwards (proof the server runs the mod) pushes our saved client preferences to the server. */
    private static boolean applyPending;

    private CmsAllClient() {
    }

    public static void init() {
        KeyBindings.registerKeyBinding(TOGGLE_KEY);

        ClientConfigView.setSyncListener(CmsAllClient::onServerSync);
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> applyPending = true);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> applyPending = false);

        GuiEvent.INIT_POST.register((screen, widgets, children) -> {
            if (screen.getClass() != PauseScreen.class) {
                return; // exact match: skip subclasses/other pause-like screens
            }
            AbstractWidget ref = findButton(screen, "gui.advancements");
            int size = ref != null ? ref.getHeight() : 20;
            int y = ref != null ? ref.y : screen.height / 4 + 32;
            int x = ref != null ? ref.x - size - 4 : screen.width / 2 - 126;
            IconButton button = new IconButton(x, y, size, new ItemStack(Items.IRON_PICKAXE),
                    b -> Minecraft.getInstance().setScreen(new CmsAllConfigScreen(screen)));
            widgets.add(button);
            children.add(button);
        });

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (TOGGLE_KEY.consumeClick()) {
                runCommand("cmsall toggle");
            }
        });
    }

    /** On the first server config sync after joining, push our saved client prefs to the server. */
    private static void onServerSync() {
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
        if (!cfg.protect) { // server default is ON, so only push when the player turned it off
            runCommand("cmsall protect false");
        }
    }

    /** Finds a vanilla button on the screen by its translation key, or null. */
    private static AbstractWidget findButton(Screen screen, String translationKey) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget) {
                AbstractWidget w = (AbstractWidget) child;
                Component msg = w.getMessage();
                if (msg instanceof TranslatableComponent
                        && translationKey.equals(((TranslatableComponent) msg).getKey())) {
                    return w;
                }
            }
        }
        return null;
    }

    /** Runs a server command from the client (reuses the validated /cmsall path). */
    public static void runCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.chat("/" + command);
        }
    }
}
