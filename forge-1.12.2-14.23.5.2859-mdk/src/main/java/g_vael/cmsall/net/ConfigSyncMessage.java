package g_vael.cmsall.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** S2C: the server's current config snapshot, as JSON. Read/write are bounded to {@link #MAX_CONFIG_BYTES}. */
public class ConfigSyncMessage implements IMessage {

    /** bounded max bytes for the config-sync JSON — keeps the allocation small while lifting the 32767-char functional limit. */
    static final int MAX_CONFIG_BYTES = 262144;

    public String json = "";

    public ConfigSyncMessage() {
    }

    public ConfigSyncMessage(String json) {
        this.json = json;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        json = ConfigBytes.readBounded(buf, MAX_CONFIG_BYTES);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ConfigBytes.writeBounded(buf, json, MAX_CONFIG_BYTES);
    }
}
