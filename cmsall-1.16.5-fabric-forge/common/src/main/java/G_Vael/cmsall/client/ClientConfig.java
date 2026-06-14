package G_Vael.cmsall.client;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.shedaniel.architectury.platform.Platform;

import G_Vael.cmsall.CmsAll;

/** Client-only, world-independent preferences (activation mode + auto-replant + tool protection). */
public final class ClientConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ClientConfig instance;

    public String mode = "hold";
    public boolean replant = false;
    public boolean protect = true;

    private ClientConfig() {
    }

    private static Path path() {
        return Platform.getConfigFolder().resolve("cmsall-client.json");
    }

    public static ClientConfig get() {
        if (instance == null) {
            Path p = path();
            ClientConfig loaded = null;
            try {
                if (Files.exists(p)) {
                    try (Reader r = Files.newBufferedReader(p)) {
                        loaded = GSON.fromJson(r, ClientConfig.class);
                    }
                }
            } catch (Exception e) {
                CmsAll.LOGGER.warn("[CM'sALL] failed to read client config {}", p);
            }
            instance = loaded != null ? loaded : new ClientConfig();
        }
        return instance;
    }

    public static void save() {
        Path p = path();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(get(), w);
            }
        } catch (Exception e) {
            CmsAll.LOGGER.warn("[CM'sALL] failed to save client config {}", p);
        }
    }
}
