package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pings {

    public static void ping(ServerPlayer player) {
        var packet = new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_PLING, SoundSource.UI, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 0);
        player.connection.send(packet);
    }


    public static void handlePings(PlayerChatMessage message) {
        String regex = String.join("|",TwitchierChat.minecraftServer.getPlayerList().getPlayerNamesArray());
        Matcher matcher = Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(message.signedContent());

        HashSet<ServerPlayer> pings = new HashSet<>();

        while (matcher.find()) {
            ServerPlayer player = TwitchierChat.minecraftServer.getPlayerList().getPlayerByName(matcher.group());
            if(player == null) {
                continue;
            }

            UUID uuid = player.getUUID();

            if (Config.shouldPing(uuid) && !message.sender().equals(uuid)) {
                pings.add(player);
            }
        }
        pings.forEach(Pings::ping);
    }

    private static void handleCommand(CommandContext<CommandSourceStack> context, boolean shouldPing) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return;
        }
        Config.setPing(player.getUUID(), shouldPing);
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ping").requires(CommandSourceStack::isPlayer).executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                return 0;
                            }
                            boolean status = Config.shouldPing(player.getUUID());
                            context.getSource().sendSuccess(() -> Component.literal("Your pings are currently " + (status ? "on" : "off")), false);

                            return 1;
                        })
                        .then(Commands.literal("on").executes(context -> {
                            handleCommand(context, true);
                            context.getSource().sendSuccess(() -> Component.literal("Turned on your pings"), false);
                            return 1;
                        })).then(Commands.literal("off").executes(context -> {
                            handleCommand(context, false);
                            context.getSource().sendSuccess(() -> Component.literal("Turned off your pings"), false);


                            return 1;
                        }))
        );
    }
}
