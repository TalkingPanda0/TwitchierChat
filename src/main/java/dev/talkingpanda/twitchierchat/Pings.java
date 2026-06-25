package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.UUID;

public class Pings {

    public static void ping(ServerPlayer player) {
        var packet = new ClientboundSoundPacket(Config.getPingSound(player.getUUID()), SoundSource.UI, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 0);
        player.connection.send(packet);
    }


    public static void handlePings(PlayerChatMessage message, ServerPlayer receiver) {

        UUID receiverUUID = receiver.getUUID();
        UUID senderUUID = message.sender();
        if (!Config.shouldPing(receiverUUID, senderUUID, false) || senderUUID.equals(receiverUUID)) {
            return;
        }

        String name = receiver.getPlainTextName().toLowerCase();
        String content = message.signedContent().toLowerCase();
        if (content.contains(name)) {
            ping(receiver);
            return;
        }

        String[] aliases = Config.getAliases(receiverUUID);
        for (String alias : aliases) {
            if (content.contains(alias)) {
                ping(receiver);
                return;
            }
        }
    }

    private static void handleCommand(CommandContext<CommandSourceStack> context, boolean reply, boolean shouldPing) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return;
        }
        Config.setPing(player.getUUID(), reply, shouldPing);
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            boolean status = Config.shouldPing(player.getUUID(), null, false);
                            boolean replyStatus = Config.shouldPing(player.getUUID(), null, true);

                            context.getSource().sendSuccess(() -> Component.literal("Your pings are currently " + (status ? "on" : "off") + ". Your reply pings are currently " + (replyStatus ? "on" : "off")), false);

                            return 1;
                        })
                        .then(Commands.literal("on").executes(context -> {
                            handleCommand(context, false, true);
                            context.getSource().sendSuccess(() -> Component.literal("Turned on your pings"), false);
                            return 1;
                        })).then(Commands.literal("off").executes(context -> {
                            handleCommand(context, false, false);
                            context.getSource().sendSuccess(() -> Component.literal("Turned off your pings"), false);
                            return 1;
                        }))
                        .then(Commands.literal("reply")
                                .then(Commands.literal("on").executes(context -> {
                                    handleCommand(context, true, true);
                                    context.getSource().sendSuccess(() -> Component.literal("Turned on your reply pings"), false);
                                    return 1;
                                }))
                                .then(Commands.literal("off").executes(context -> {
                                    handleCommand(context, true, false);
                                    context.getSource().sendSuccess(() -> Component.literal("Turned off your reply pings"), false);
                                    return 1;
                                }))
                        )
        );

        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer).then(Commands.literal("aliases")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    String aliases = String.join(", ", Config.getAliases(player.getUUID()));
                    context.getSource().sendSuccess(() -> Component.literal("Your aliases are: " + aliases), false);

                    return 1;
                }).then(Commands.literal("add").then(Commands.argument("alias", StringArgumentType.word()).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    String alias = StringArgumentType.getString(context, "alias");
                    if (Config.addAlias(player.getUUID(), alias)) {
                        context.getSource().sendSuccess(() -> Component.literal("Added " + alias + " to your aliases"), false);
                    } else {
                        context.getSource().sendFailure(Component.literal(alias + " is already your alias"));
                    }
                    return 1;
                })))
                .then(Commands.literal("remove").then(Commands.argument("alias", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                return null;
                            }

                            return SharedSuggestionProvider.suggest(Config.getAliases(player.getUUID()), builder);
                        })
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            String alias = StringArgumentType.getString(context, "alias");
                            if (Config.removeAlias(player.getUUID(), alias)) {
                                context.getSource().sendSuccess(() -> Component.literal(alias + " is no longer your alias"), false);
                            } else {
                                context.getSource().sendFailure(Component.literal(alias + " is not your alias"));
                            }
                            return 1;
                        })))
        ));


        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer).then(Commands.literal("sound").then(Commands.argument("sound", IdentifierArgument.id())
                .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    Identifier identifier = IdentifierArgument.getId(context, "sound");
                    Config.setPingSound(player.getUUID(), identifier);
                    context.getSource().sendSuccess(() -> Component.literal("Set your ping sound to " + identifier.toShortString()), false);

                    return 1;
                })
        )));

        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer).then(Commands.literal("resetSound").executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                return 0;
            }
            Config.setPingSound(player.getUUID(), null);
            return 1;
        })));

        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer)
                .then(Commands.literal("block")
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .suggests((c, p) -> {
                                    PlayerList list = (c.getSource()).getServer().getPlayerList();
                                    return SharedSuggestionProvider.suggest(list.getPlayers().stream().map(Player::nameAndId).map(NameAndId::name), p);
                                }).executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player == null) {
                                        return 0;
                                    }
                                    Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
                                    for (NameAndId p : players) {
                                        if (Config.blockPlayer(player.getUUID(), p.id())) {
                                            context.getSource().sendSuccess(() -> Component.literal(p.name() + " is now blocked"), false);
                                        } else {
                                            context.getSource().sendFailure(Component.literal(p.name() + " is already blocked"));
                                        }
                                    }
                                    return 1;
                                })
                        ))
                .then(Commands.literal("unblock").then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .suggests((c, p) -> {
                            PlayerList list = (c.getSource()).getServer().getPlayerList();
                            return SharedSuggestionProvider.suggest(list.getPlayers().stream().map(Player::nameAndId).map(NameAndId::name), p);
                        }).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
                            for (NameAndId p : players) {
                                if (Config.unblockPlayer(player.getUUID(), p.id())) {
                                    context.getSource().sendSuccess(() -> Component.literal(p.name() + " is now unblocked"), false);
                                } else {
                                    context.getSource().sendFailure(Component.literal(p.name() + " is not blocked"));
                                }
                            }
                            return 1;
                        })))
        );
    }
}
