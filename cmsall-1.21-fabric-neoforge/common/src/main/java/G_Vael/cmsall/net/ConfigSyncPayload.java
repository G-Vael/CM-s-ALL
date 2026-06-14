package G_Vael.cmsall.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import G_Vael.cmsall.CmsAll;

/** S2C: full Layer-2 config snapshot. */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CmsAll.MOD_ID, "config_sync"));

    /** bounded max bytes for the config-sync JSON — keeps the allocation small while lifting the 32767-char functional limit. */
    public static final int MAX_CONFIG_BYTES = 262144;

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(MAX_CONFIG_BYTES), ConfigSyncPayload::json, ConfigSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
