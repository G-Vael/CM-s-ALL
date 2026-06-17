package g_vael.cmsall.command;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import g_vael.cmsall.config.ConfigManager;
import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.ActivationMode;
import g_vael.cmsall.core.ActivationState;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.core.PlacedBlocksTracker;
import g_vael.cmsall.core.RuntimeConfig;
import g_vael.cmsall.net.CmsAllNetwork;

/** /cmsall... command. */
public class CmsAllCommand extends CommandBase {

    private static final List<String> SUBS = Arrays.asList(
            "reload", "enable", "status", "mode", "toggle", "replant", "protect", "track", "mine", "cut", "dig");

    @Override
    public String getName() {
        return "cmsall";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/cmsall <reload|enable|status|mode|toggle|replant|protect|mine|cut|dig> ...";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // personal sub-commands work for everyone; edits are gated inside
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new CommandException(getUsage(sender));
        }
        String sub = args[0].toLowerCase();
        if ("status".equals(sub)) {
            status(sender);
        } else if ("reload".equals(sub)) {
            requireEdit(server, sender);
            ConfigManager.load();
            PlacedBlocksTracker.trim(server);
            CmsAllNetwork.syncToAll(server);
            msg(sender, "config reloaded");
        } else if ("enable".equals(sub)) {
            requireEdit(server, sender);
            boolean value = parseBoolean(arg(args, 1));
            ConfigManager.server().enabled = value;
            persist(server);
            msg(sender, "master switch = " + value);
        } else if ("mode".equals(sub)) {
            setMode(getCommandSenderAsPlayer(sender), arg(args, 1));
        } else if ("toggle".equals(sub)) {
            toggle(getCommandSenderAsPlayer(sender));
        } else if ("replant".equals(sub)) {
            setReplant(getCommandSenderAsPlayer(sender), args.length > 1 ? Boolean.valueOf(parseBoolean(args[1])) : null);
        } else if ("protect".equals(sub)) {
            setProtect(getCommandSenderAsPlayer(sender), args.length > 1 ? Boolean.valueOf(parseBoolean(args[1])) : null);
        } else if ("track".equals(sub)) {
            track(server, sender, args);
        } else if ("mine".equals(sub) || "cut".equals(sub) || "dig".equals(sub)) {
            requireEdit(server, sender);
            editList(server, sender, kindOf(sub), arg(args, 1), arg(args, 2), arg(args, 3));
        } else {
            throw new CommandException(getUsage(sender));
        }
    }

    private static Functions.Kind kindOf(String sub) {
        if ("cut".equals(sub)) {
            return Functions.Kind.CUT;
        }
        if ("dig".equals(sub)) {
            return Functions.Kind.DIG;
        }
        return Functions.Kind.MINE;
    }

    private void editList(MinecraftServer server, ICommandSender sender, Functions.Kind kind,
                          String type, String op, String id) throws CommandException {
        boolean tool = "tool".equalsIgnoreCase(type);
        boolean add = "add".equalsIgnoreCase(op);
        if (!tool && !"block".equalsIgnoreCase(type)) {
            throw new CommandException("Expected 'block' or 'tool'");
        }
        if (!add && !"remove".equalsIgnoreCase(op)) {
            throw new CommandException("Expected 'add' or 'remove'");
        }
        List<String> list = listFor(kind, tool);
        String norm = id.contains(":") ? id : "minecraft:" + id;
        if (add) {
            if (!list.contains(norm)) {
                list.add(norm);
            }
        } else {
            list.remove(norm);
        }
        persist(server);
        msg(sender, kind.name().toLowerCase() + " " + (tool ? "tool" : "block")
                + (add ? " added: " : " removed: ") + norm + " (" + list.size() + ")");
    }

    private static List<String> listFor(Functions.Kind kind, boolean tool) {
        ServerConfig c = ConfigManager.server();
        switch (kind) {
            case MINE:
                return tool ? c.mineTools : c.mineBlocks;
            case CUT:
                return tool ? c.cutTools : c.cutBlocks;
            case DIG:
                return tool ? c.digTools : c.digBlocks;
            default:
                return c.mineBlocks;
        }
    }

    private void track(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String op = arg(args, 1).toLowerCase();
        if ("status".equals(op)) {
            trackStatus(sender);
            return;
        }
        if ("reset".equals(op)) {
            requireEdit(server, sender);
            String which = arg(args, 2).toLowerCase();
            Functions.Kind kind = "all".equals(which) ? null : kindOrThrow(which);
            PlacedBlocksTracker.resetWorlds(server, kind);
            sender.sendMessage(new TextComponentTranslation("cmsall.msg.track_reset", which));
            return;
        }
        if ("max".equals(op)) {
            requireEdit(server, sender);
            Functions.Kind kind = kindOrThrow(arg(args, 2).toLowerCase());
            int max = Math.min(131072, Math.max(1, parseInt(arg(args, 3))));
            ServerConfig c = ConfigManager.server();
            switch (kind) {
                case MINE:
                    c.trackMineMax = max;
                    break;
                case CUT:
                    c.trackCutMax = max;
                    break;
                case DIG:
                    c.trackDigMax = max;
                    break;
            }
            ConfigManager.save();
            ConfigManager.apply();
            PlacedBlocksTracker.trim(server);
            CmsAllNetwork.syncToAll(server);
            sender.sendMessage(new TextComponentTranslation("cmsall.msg.track_max", kind.name().toLowerCase(), Integer.valueOf(max)));
            return;
        }
        if ("overflow".equals(op)) {
            requireEdit(server, sender);
            String mode = arg(args, 2).toLowerCase();
            if (!"evict".equals(mode) && !"stop".equals(mode)) {
                throw new CommandException("Expected 'evict' or 'stop'");
            }
            ConfigManager.server().trackOverflow = mode;
            persist(server);
            sender.sendMessage(new TextComponentTranslation("cmsall.msg.track_overflow", mode));
            return;
        }
        requireEdit(server, sender);
        Functions.Kind kind = kindOrThrow(op);
        boolean value = parseBoolean(arg(args, 2));
        ServerConfig c = ConfigManager.server();
        switch (kind) {
            case MINE:
                c.trackMine = value;
                break;
            case CUT:
                c.trackCut = value;
                break;
            case DIG:
                c.trackDig = value;
                break;
        }
        persist(server);
        sender.sendMessage(new TextComponentTranslation("cmsall.msg.track",
                kind.name().toLowerCase(), new TextComponentTranslation(value ? "cmsall.gui.on" : "cmsall.gui.off")));
    }

    private void trackStatus(ICommandSender sender) {
        World world = sender.getEntityWorld();
        for (Functions.Kind kind : Functions.Kind.values()) {
            int count = PlacedBlocksTracker.count(world, kind);
            int max = RuntimeConfig.trackMax[kind.ordinal()];
            int pct = max > 0 ? count * 100 / max : 0;
            boolean on = RuntimeConfig.trackEnabled[kind.ordinal()];
            msg(sender, kind.name().toLowerCase() + ": " + count + "/" + max
                    + " (" + pct + "%) [" + (on ? "ON" : "OFF") + "]");
        }
    }

    private static Functions.Kind kindOrThrow(String name) throws CommandException {
        if ("mine".equals(name)) {
            return Functions.Kind.MINE;
        }
        if ("cut".equals(name)) {
            return Functions.Kind.CUT;
        }
        if ("dig".equals(name)) {
            return Functions.Kind.DIG;
        }
        throw new CommandException("Expected 'mine', 'cut' or 'dig'");
    }

    private void setMode(EntityPlayerMP player, String raw) {
        String key = raw.toLowerCase();
        if (!RuntimeConfig.allowedModes.contains(key)) {
            player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.mode_denied", key), true);
            return;
        }
        ActivationMode mode = ActivationMode.parse(key, RuntimeConfig.defaultMode);
        ActivationState.setMode(player, mode);
        player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.mode",
                new TextComponentTranslation("cmsall.mode." + mode.id())), true);
    }

    private void toggle(EntityPlayerMP player) {
        if (ActivationState.mode(player) != ActivationMode.TOGGLE) {
            player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.toggle_denied"), true);
            return;
        }
        boolean on = ActivationState.flipToggle(player);
        player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.toggle",
                new TextComponentTranslation(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
    }

    private void setReplant(EntityPlayerMP player, Boolean value) {
        boolean on = value != null ? value.booleanValue() : ActivationState.flipReplant(player);
        if (value != null) {
            ActivationState.setReplant(player, value.booleanValue());
        }
        player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.replant",
                new TextComponentTranslation(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
    }

    private void setProtect(EntityPlayerMP player, Boolean value) {
        boolean on = value != null ? value.booleanValue() : ActivationState.flipProtectTool(player);
        if (value != null) {
            ActivationState.setProtectTool(player, value.booleanValue());
        }
        player.sendStatusMessage(new TextComponentTranslation("cmsall.msg.protect",
                new TextComponentTranslation(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
    }

    private void status(ICommandSender sender) {
        msg(sender, "master = " + ConfigManager.server().enabled
                + ", maxBlocks = " + ConfigManager.server().globalMaxBlocks);
    }

    private void requireEdit(MinecraftServer server, ICommandSender sender) throws CommandException {
        if (!canEdit(server, sender)) {
            throw new CommandException("commands.generic.permission");
        }
    }

    private boolean canEdit(MinecraftServer server, ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            if (server.isSinglePlayer() && server.getServerOwner() != null
                    && server.getServerOwner().equalsIgnoreCase(player.getGameProfile().getName())) {
                return true;
            }
        }
        return sender.canUseCommand(ConfigManager.server().editPermissionLevel, "cmsall");
    }

    private void persist(MinecraftServer server) {
        ConfigManager.save();
        ConfigManager.apply();
        CmsAllNetwork.syncToAll(server);
    }

    private static String arg(String[] args, int i) throws CommandException {
        if (i >= args.length) {
            throw new CommandException("Missing argument");
        }
        return args[i];
    }

    private void msg(ICommandSender sender, String message) {
        sender.sendMessage(new TextComponentString("[CM'sALL] " + message));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
                                          @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, SUBS);
        }
        return java.util.Collections.<String>emptyList();
    }
}
