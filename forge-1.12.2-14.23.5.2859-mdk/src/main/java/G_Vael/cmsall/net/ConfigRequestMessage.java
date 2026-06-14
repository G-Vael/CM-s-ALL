package G_Vael.cmsall.net;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** C2S: a client asks for a fresh config + counts snapshot (refreshes the tracking screen). */
public class ConfigRequestMessage implements IMessage {

    public ConfigRequestMessage() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static final class Handler implements IMessageHandler<ConfigRequestMessage, IMessage> {
        @Override
        public IMessage onMessage(ConfigRequestMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            IThreadListener thread = FMLCommonHandler.instance().getWorldThread(ctx.netHandler);
            thread.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (CmsAllNetwork.allowResync(player.getServer(), player)) {
                        CmsAllNetwork.syncTo(player);
                    }
                }
            });
            return null;
        }
    }
}
