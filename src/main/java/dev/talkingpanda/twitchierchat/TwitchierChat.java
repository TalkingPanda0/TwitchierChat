package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.talkingpanda.twitchierchat.parsers.EmoteParser;
import dev.talkingpanda.twitchierchat.parsers.PlayerParser;
import dev.talkingpanda.twitchierchat.parsers.TextParser;
import dev.talkingpanda.twitchierchat.parsers.UrlParser;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Player;
import offsetmonkey538.meshlib.api.HttpHandlerRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class TwitchierChat implements DedicatedServerModInitializer {
    public static final Path configDir = Path.of("./config/twitchierchat");


    public final static Logger LOGGER = LoggerFactory.getLogger("twitchierchat");
    private static final List<TextParser> PARSERS = List.of(new EmoteParser(), new PlayerParser(), new UrlParser());

    public static DedicatedServer minecraftServer;


    public static @Nullable MutableComponent formatText(Component text) {
        if (PARSERS == null || text == null) return null;
        var literal = text.tryCollapseToString();
        if (literal == null || literal.isBlank()) return null;
        for (var parser : PARSERS) {
            literal = parser.undo(literal);
        }
        return formatString(literal);
    }

    public static @Nullable MutableComponent formatString(@Nullable String content) {

        if (content == null) return null;
        MutableComponent result = Component.empty();
        boolean modified = false;
        String[] inputs = content.split(" ");
        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];

            @Nullable Component parserResult = null;
            for (var parser : PARSERS) {
                parserResult = parser.parse(input);
                if (parserResult != null) break;
            }
            if (parserResult == null) {
                result.append(Component.literal(input));
            } else {
                modified = true;
                result.append(parserResult);
            }
            if (i != inputs.length - 1) {
                result.append(" ");
            }
        }

        if (modified) return result;

        return null;
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("twitchierchat").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)).then(
                Commands.literal("managers").then(Commands.literal("add").then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .suggests((c, p) -> {
                                    PlayerList list = (c.getSource()).getServer().getPlayerList();
                                    return SharedSuggestionProvider.suggest(list.getPlayers().stream().map(Player::nameAndId).map(NameAndId::name), p);
                                }).executes(context -> {
                                    Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
                                    for (NameAndId player : players) {
                                        if (Config.addManager(player.id())) {
                                            context.getSource().sendSuccess(() -> Component.literal(player.name() + " is now a manager"), true);
                                        } else {
                                            context.getSource().sendFailure(Component.literal(player.name() + " is already a manager"));
                                        }
                                    }
                                    return 1;
                                })))
                        .then(Commands.literal("remove").then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .suggests((c, p) -> {
                                    PlayerList list = (c.getSource()).getServer().getPlayerList();
                                    return SharedSuggestionProvider.suggest(list.getPlayers().stream().map(Player::nameAndId).map(NameAndId::name), p);
                                }).executes(context -> {
                                    Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
                                    for (NameAndId player : players) {
                                        if (Config.removeManager(player.id())) {
                                            context.getSource().sendSuccess(() -> Component.literal(player.name() + " is no longer a manager"), true);
                                        } else {
                                            context.getSource().sendFailure(Component.literal(player.name() + " is not a manager"));

                                        }
                                    }
                                    return 1;
                                }))))
        );

        dispatcher.register(Commands.literal("twitchierchat").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)).then(Commands.literal("server").then(
                Commands.argument("address", StringArgumentType.string()).executes(context -> {
                    String address = StringArgumentType.getString(context, "address");
                    Config.setServer(address, TwitchierChat.minecraftServer.getServerPort());
                    context.getSource().sendSuccess(() -> Component.literal("Set resource pack address to " + Config.getServerUrl()), true);
                    return 1;
                }).then(Commands.argument("port", IntegerArgumentType.integer())
                        .suggests((c, p) -> p.suggest(TwitchierChat.minecraftServer.getServerPort()).buildFuture())
                        .executes(context -> {
                            String address = StringArgumentType.getString(context, "address");
                            Integer port = IntegerArgumentType.getInteger(context, "port");
                            Config.setServer(address, port);
                            context.getSource().sendSuccess(() -> Component.literal("Set resource pack address to " + Config.getServerUrl()), true);
                            return 1;
                        }))
        )));

        dispatcher.register(Commands.literal("twitchierchat").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)).then(Commands.literal("maxReplyHistory").executes(context -> {
            context.getSource().sendSuccess(() -> Component.literal("Max reply history is currently: " + Config.getMaxHistory()), false);
            return 1;
        }).then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(context -> {
            Integer count = IntegerArgumentType.getInteger(context, "count");
            Config.setMaxHistory(count);
            context.getSource().sendSuccess(() -> Component.literal("Set max reply history to " + count), true);
            return 1;
        }))));

        dispatcher.register(Commands.literal("twitchierchat").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)).then(Commands.literal("reload").executes(
                context -> {
                    try {
                        Config.readConfig();
                    } catch (Exception e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()).withColor(CommonColors.RED));
                        return 0;
                    }
                    context.getSource().sendSuccess(() -> Component.literal("Reloaded config file"), true);
                    return 1;
                }
        )));

    }

    private void readConfig() {
        try {
            Files.createDirectories(configDir);
            Config.readConfig();
            Emotes.readConfig();
        } catch (Exception e) {
            TwitchierChat.LOGGER.error("Failed to read config: ", e);
        }
    }

    @Override
    public void onInitializeServer() {


        HttpHandlerRegistry.INSTANCE.register("emotes.zip", new ResourcePackHandler());


        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) ->
        {
            Emotes.registerCommand(dispatcher);
            NameColor.registerCommand(dispatcher);
            Replies.registerCommand(dispatcher);
            Pings.registerCommand(dispatcher);
            TwitchierChat.registerCommand(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(this::onLogicalServerStarting);

    }


    private void onLogicalServerStarting(MinecraftServer server) {

        if (!(server instanceof DedicatedServer)) {
            TwitchierChat.LOGGER.error("Not running on a dedicated server");
            return;
        }

        minecraftServer = (DedicatedServer) server;
        readConfig();
    }
}
