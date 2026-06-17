package g_vael.cmsall.net;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

/**
 * Bounded length-prefixed UTF-8 string codec for the config messages (V-01/V-02).
 *
 * <p>Mirrors 1.21's {@code FriendlyByteBuf.writeUtf(s, max)/readUtf(max)} on a raw {@link ByteBuf}: a varint byte length
 * prefix followed by the UTF-8 bytes, with both the declared length AND the readable-bytes checked against {@code maxBytes}
 * so a hostile/garbled packet can't make us allocate a huge buffer before validation.
 */
final class ConfigBytes {

    private ConfigBytes() {
    }

    static void writeBounded(ByteBuf buf, String s, int maxBytes) {
        byte[] bytes = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maxBytes) {
            throw new EncoderException("config string too long: " + bytes.length + " > " + maxBytes);
        }
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    static String readBounded(ByteBuf buf, int maxBytes) {
        int len = readVarInt(buf);
        if (len < 0 || len > maxBytes) {
            throw new DecoderException("config string length out of bounds: " + len);
        }
        if (len > buf.readableBytes()) {
            throw new DecoderException("config string truncated: declared " + len + ", available " + buf.readableBytes());
        }
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    private static int readVarInt(ByteBuf buf) {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = buf.readByte();
            value |= (b & 127) << shift;
            shift += 7;
            if (shift > 35) {
                throw new DecoderException("VarInt too big");
            }
        } while ((b & 128) == 128);
        return value;
    }
}
