package g_vael.cmsall.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import g_vael.cmsall.CmsAll;

/** C2S: an admin's edited config. */
public record ConfigEditPayload(String json) implements CustomPacketPayload {

    public static final Type<ConfigEditPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CmsAll.MOD_ID, "config_edit"));

    // bound the config-sync JSON to keep the pre-auth allocation small (V-01/V-02).
    public static final StreamCodec<FriendlyByteBuf, ConfigEditPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(CmsAllNetwork.MAX_CONFIG_BYTES),
                    ConfigEditPayload::json, ConfigEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
