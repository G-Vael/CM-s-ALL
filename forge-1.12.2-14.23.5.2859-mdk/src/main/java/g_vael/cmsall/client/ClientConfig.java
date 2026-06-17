package g_vael.cmsall.client;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.common.Loader;

import g_vael.cmsall.CmsAll;

/** Client-only, world-independent preferences (mode + replant + protect), saved to config/cmsall-client.json, so they carry across every world/server. */
public final class ClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig instance;

    public String mode = "hold";
    public boolean replant = false;
    public boolean protect = true;

    private ClientConfig() {
    }

    private static File file() {
        return new File(Loader.instance().getConfigDir(), "cmsall-client.json");
    }

    public static ClientConfig get() {
        if (instance == null) {
            File p = file();
            ClientConfig loaded = null;
            try {
                if (p.exists()) {
                    Reader r = new FileReader(p);
                    try {
                        loaded = GSON.fromJson(r, ClientConfig.class);
                    } finally {
                        r.close();
                    }
                }
            } catch (Exception e) {
                CmsAll.LOGGER.warn("[CM'sALL] failed to read client config");
            }
            instance = loaded != null ? loaded : new ClientConfig();
        }
        return instance;
    }

    public static void save() {
        File p = file();
        try {
            if (p.getParentFile() != null) {
                p.getParentFile().mkdirs();
            }
            Writer w = new FileWriter(p);
            try {
                GSON.toJson(get(), w);
            } finally {
                w.close();
            }
        } catch (Exception e) {
            CmsAll.LOGGER.warn("[CM'sALL] failed to save client config");
        }
    }
}
