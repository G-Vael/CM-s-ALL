package g_vael.cmsall.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

/** C2S: an edited config submitted from the GUI for server-side validation. Read/write are bounded to {@link ConfigSyncMessage#MAX_CONFIG_BYTES}. */
public class ConfigEditMessage implements IMessage {

    public String json = "";

    public ConfigEditMessage() {
    }

    public ConfigEditMessage(String json) {
        this.json = json;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        json = ConfigBytes.readBounded(buf, ConfigSyncMessage.MAX_CONFIG_BYTES);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ConfigBytes.writeBounded(buf, json, ConfigSyncMessage.MAX_CONFIG_BYTES);
    }
}
