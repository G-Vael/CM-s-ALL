package g_vael.cmsall.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import g_vael.cmsall.config.ConfigManager;
import g_vael.cmsall.config.ServerConfig;
import g_vael.cmsall.core.ActivationMode;
import g_vael.cmsall.core.ActivationState;
import g_vael.cmsall.core.Functions;
import g_vael.cmsall.core.PlacedBlocksTracker;
import g_vael.cmsall.core.RuntimeConfig;
import g_vael.cmsall.net.CmsAllNetwork;

/** /cmsall... commands. */
public final class CmsAllCommand {

    private CmsAllCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cmsall")
                .then(Commands.literal("reload")
                        .requires(CmsAllCommand::canEdit)
                        .executes(ctx -> {
                            ConfigManager.load();
                            PlacedBlocksTracker.trim(ctx.getSource().getServer());
                            CmsAllNetwork.syncToAll(ctx.getSource().getServer());
                            feedback(ctx, "config reloaded");
                            return 1;
                        }))
                .then(Commands.literal("enable")
                        .requires(CmsAllCommand::canEdit)
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean value = BoolArgumentType.getBool(ctx, "value");
                                    ConfigManager.server().enabled = value;
                                    ConfigManager.save();
                                    ConfigManager.apply();
                                    CmsAllNetwork.syncToAll(ctx.getSource().getServer());
                                    feedback(ctx, "master switch = " + value);
                                    return 1;
                                })))
                .then(Commands.literal("status")
                        .executes(ctx -> {
                            status(ctx);
                            return 1;
                        }))
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .executes(ctx -> setMode(ctx, StringArgumentType.getString(ctx, "mode")))))
                .then(Commands.literal("toggle")
                        .executes(CmsAllCommand::toggle))
                .then(Commands.literal("replant")
                        .executes(ctx -> setReplant(ctx, null))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setReplant(ctx, BoolArgumentType.getBool(ctx, "value")))))
                .then(Commands.literal("protect")
                        .executes(ctx -> setProtect(ctx, null))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setProtect(ctx, BoolArgumentType.getBool(ctx, "value")))))
                .then(trackCommand())
                .then(funcCommand("mine", Functions.Kind.MINE))
                .then(funcCommand("cut", Functions.Kind.CUT))
                .then(funcCommand("dig", Functions.Kind.DIG)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> trackCommand() {
        LiteralArgumentBuilder<CommandSourceStack> track = Commands.literal("track");
        for (Functions.Kind kind : Functions.Kind.values()) {
            track.then(Commands.literal(kind.name().toLowerCase())
                    .requires(CmsAllCommand::canEdit)
                    .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> setTrack(ctx, kind, BoolArgumentType.getBool(ctx, "value")))));
        }
        track.then(Commands.literal("max").requires(CmsAllCommand::canEdit)
                .then(Commands.literal("mine").then(trackMaxArg(Functions.Kind.MINE)))
                .then(Commands.literal("cut").then(trackMaxArg(Functions.Kind.CUT)))
                .then(Commands.literal("dig").then(trackMaxArg(Functions.Kind.DIG))));
        track.then(Commands.literal("overflow").requires(CmsAllCommand::canEdit)
                .then(Commands.literal("evict").executes(ctx -> setOverflow(ctx, "evict")))
                .then(Commands.literal("stop").executes(ctx -> setOverflow(ctx, "stop"))));
        LiteralArgumentBuilder<CommandSourceStack> reset = Commands.literal("reset").requires(CmsAllCommand::canEdit);
        for (Functions.Kind kind : Functions.Kind.values()) {
            reset.then(Commands.literal(kind.name().toLowerCase()).executes(ctx -> resetTrack(ctx, kind)));
        }
        reset.then(Commands.literal("all").executes(ctx -> resetTrack(ctx, null)));
        track.then(reset);
        track.then(Commands.literal("status").executes(CmsAllCommand::trackStatus));
        return track;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> trackMaxArg(Functions.Kind kind) {
        return Commands.argument("max", IntegerArgumentType.integer(1, 131072))
                .executes(ctx -> setTrackMax(ctx, kind, IntegerArgumentType.getInteger(ctx, "max")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> funcCommand(String name, Functions.Kind kind) {
        return Commands.literal(name).requires(CmsAllCommand::canEdit)
                .then(typeCommand("block", kind, false))
                .then(typeCommand("tool", kind, true));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> typeCommand(String name, Functions.Kind kind, boolean tool) {
        return Commands.literal(name)
                .then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> editList(ctx, kind, tool, StringArgumentType.getString(ctx, "id"), true))))
                .then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> editList(ctx, kind, tool, StringArgumentType.getString(ctx, "id"), false))));
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

    private static int editList(CommandContext<CommandSourceStack> ctx, Functions.Kind kind, boolean tool,
                                String id, boolean add) {
        List<String> list = listFor(kind, tool);
        String norm = id.contains(":") ? id : "minecraft:" + id;
        if (add) {
            if (!list.contains(norm)) {
                list.add(norm);
            }
        } else {
            list.removeIf(norm::equals);
        }
        ConfigManager.save();
        ConfigManager.apply();
        CmsAllNetwork.syncToAll(ctx.getSource().getServer());
        feedback(ctx, kind.name().toLowerCase() + " " + (tool ? "tool" : "block")
                + (add ? " added: " : " removed: ") + norm + " (" + list.size() + ")");
        return 1;
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, String raw) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String key = raw.toLowerCase();
        if (!RuntimeConfig.allowedModes.contains(key)) {
            player.displayClientMessage(new TranslatableComponent("cmsall.msg.mode_denied", key), true);
            return 0;
        }
        ActivationMode mode = ActivationMode.parse(key, RuntimeConfig.defaultMode);
        ActivationState.setMode(player, mode);
        player.displayClientMessage(
                new TranslatableComponent("cmsall.msg.mode", new TranslatableComponent("cmsall.mode." + mode.id())), true);
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        // don't pretend to switch — tell the player to pick Toggle mode first.
        if (ActivationState.mode(player) != ActivationMode.TOGGLE) {
            player.displayClientMessage(new TranslatableComponent("cmsall.msg.toggle_denied"), true);
            return 0;
        }
        boolean on = ActivationState.flipToggle(player);
        player.displayClientMessage(
                new TranslatableComponent("cmsall.msg.toggle", new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
        return 1;
    }

    private static int setReplant(CommandContext<CommandSourceStack> ctx, Boolean value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean on = value != null ? value : ActivationState.flipReplant(player);
        if (value != null) {
            ActivationState.setReplant(player, value);
        }
        player.displayClientMessage(
                new TranslatableComponent("cmsall.msg.replant", new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
        return 1;
    }

    private static int setProtect(CommandContext<CommandSourceStack> ctx, Boolean value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean on = value != null ? value : ActivationState.flipProtectTool(player);
        if (value != null) {
            ActivationState.setProtectTool(player, value);
        }
        player.displayClientMessage(
                new TranslatableComponent("cmsall.msg.protect", new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off")), true);
        return 1;
    }

    private static void setTrackEnabled(Functions.Kind kind, boolean value) {
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
    }

    private static int setTrack(CommandContext<CommandSourceStack> ctx, Functions.Kind kind, boolean value) {
        setTrackEnabled(kind, value);
        ConfigManager.save();
        ConfigManager.apply();
        CmsAllNetwork.syncToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(new TranslatableComponent("cmsall.msg.track",
                kind.name().toLowerCase(), onOff(value)), true);
        return 1;
    }

    private static int setTrackMax(CommandContext<CommandSourceStack> ctx, Functions.Kind kind, int max) {
        int clamped = Math.min(131072, Math.max(1, max));
        ServerConfig c = ConfigManager.server();
        switch (kind) {
            case MINE:
                c.trackMineMax = clamped;
                break;
            case CUT:
                c.trackCutMax = clamped;
                break;
            case DIG:
                c.trackDigMax = clamped;
                break;
        }
        ConfigManager.save();
        ConfigManager.apply();
        PlacedBlocksTracker.trim(ctx.getSource().getServer());
        CmsAllNetwork.syncToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(new TranslatableComponent("cmsall.msg.track_max",
                kind.name().toLowerCase(), clamped), true);
        return 1;
    }

    private static int setOverflow(CommandContext<CommandSourceStack> ctx, String mode) {
        ConfigManager.server().trackOverflow = mode;
        ConfigManager.save();
        ConfigManager.apply();
        CmsAllNetwork.syncToAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(new TranslatableComponent("cmsall.msg.track_overflow", mode), true);
        return 1;
    }

    private static int resetTrack(CommandContext<CommandSourceStack> ctx, Functions.Kind kindOrNull) {
        PlacedBlocksTracker.resetWorlds(ctx.getSource().getServer(), kindOrNull);
        String name = kindOrNull == null ? "all" : kindOrNull.name().toLowerCase();
        ctx.getSource().sendSuccess(new TranslatableComponent("cmsall.msg.track_reset", name), true);
        return 1;
    }

    private static int trackStatus(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        for (Functions.Kind kind : Functions.Kind.values()) {
            int count = PlacedBlocksTracker.count(level, kind);
            int max = RuntimeConfig.trackMax[kind.ordinal()];
            int pct = max > 0 ? count * 100 / max : 0;
            boolean on = RuntimeConfig.trackEnabled[kind.ordinal()];
            feedback(ctx, kind.name().toLowerCase() + ": " + count + "/" + max
                    + " (" + pct + "%) [" + (on ? "ON" : "OFF") + "]");
        }
        return 1;
    }

    private static Component onOff(boolean on) {
        return new TranslatableComponent(on ? "cmsall.gui.on" : "cmsall.gui.off");
    }

    private static boolean canEdit(CommandSourceStack source) {
        Entity e = source.getEntity();
        if (e instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) e;
            if (source.getServer().isSingleplayerOwner(player.getGameProfile())) {
                return true; // the single-player host owns the world (no-cheats leaves them below op level)
            }
        }
        return source.hasPermission(ConfigManager.server().editPermissionLevel);
    }

    private static void status(CommandContext<CommandSourceStack> ctx) {
        feedback(ctx, "master = " + ConfigManager.server().enabled
                + ", maxBlocks = " + ConfigManager.server().globalMaxBlocks);
    }

    private static void feedback(CommandContext<CommandSourceStack> ctx, String message) {
        ctx.getSource().sendSuccess(new TextComponent("[CM'sALL] " + message), true);
    }
}
