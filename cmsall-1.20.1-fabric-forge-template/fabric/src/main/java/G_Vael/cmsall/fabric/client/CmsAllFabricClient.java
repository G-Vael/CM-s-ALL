package G_Vael.cmsall.fabric.client;

import net.fabricmc.api.ClientModInitializer;

import G_Vael.cmsall.client.CmsAllClient;

public final class CmsAllFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CmsAllClient.init();
    }
}
