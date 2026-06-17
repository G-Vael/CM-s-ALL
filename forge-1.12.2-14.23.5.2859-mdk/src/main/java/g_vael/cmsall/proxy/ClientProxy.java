package g_vael.cmsall.proxy;

import g_vael.cmsall.client.CmsAllClient;

/** Client proxy — sets up the keybind, pause-screen button and config GUI. */
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        CmsAllClient.preInit();
    }

    @Override
    public void init() {
        CmsAllClient.init();
    }
}
