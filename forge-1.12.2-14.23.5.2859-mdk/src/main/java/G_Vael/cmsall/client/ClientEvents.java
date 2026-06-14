package G_Vael.cmsall.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/** Client event wiring: keybind, pause-screen launch button, GUI open, join detection. */
public class ClientEvents {

    private static final int BUTTON_ID = 0x6D5A11;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        while (CmsAllClient.TOGGLE_KEY.isPressed()) {
            CmsAllClient.runCommand("cmsall toggle");
        }
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiIngameMenu)) {
            return;
        }
        // Top-left corner so it never overlaps the vanilla pause buttons at any GUI scale.
        event.getButtonList().add(new IconButton(BUTTON_ID, 6, 6, 20, new ItemStack(Items.IRON_PICKAXE)));
    }

    @SubscribeEvent
    public void onAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        // re-check the open GUI is the pause menu so we don't react to a foreign button with the same id.
        if (event.getButton() != null && event.getButton().id == BUTTON_ID
                && event.getGui() instanceof GuiIngameMenu) {
            Minecraft.getMinecraft().displayGuiScreen(new CmsAllConfigGui(event.getGui()));
        }
    }

    @SubscribeEvent
    public void onConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        CmsAllClient.applyPending = true;
    }
}
