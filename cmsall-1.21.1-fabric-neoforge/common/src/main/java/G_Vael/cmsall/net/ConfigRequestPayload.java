package G_Vael.cmsall.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import G_Vael.cmsall.CmsAll;

/** C2S: a client asks for a fresh config + counts snapshot (refreshes the tracking screen). */
public record ConfigRequestPayload() implements CustomPacketPayload {

    public static final Type<ConfigRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CmsAll.MOD_ID, "config_resync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigRequestPayload> STREAM_CODEC =
            StreamCodec.unit(new ConfigRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
