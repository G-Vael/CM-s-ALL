package g_vael.cmsall.fabric.client;

import net.fabricmc.api.ClientModInitializer;

import g_vael.cmsall.client.CmsAllClient;

public final class CmsAllFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CmsAllClient.init();
    }
}
