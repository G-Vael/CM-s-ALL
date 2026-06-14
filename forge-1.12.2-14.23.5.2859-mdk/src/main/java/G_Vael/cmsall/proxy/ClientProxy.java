package G_Vael.cmsall.proxy;

import G_Vael.cmsall.client.CmsAllClient;

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
