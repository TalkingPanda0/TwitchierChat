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

import java.util.UUID;

public class Pings {

    public static void ping(ServerPlayer player) {
        var packet = new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_PLING, SoundSource.UI, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 0);
        player.connection.send(packet);
    }


    public static void handlePings(PlayerChatMessage message, ServerPlayer receiver) {

        UUID receiverUUID = receiver.getUUID();

        if (!Config.shouldPing(receiverUUID) || message.sender().equals(receiverUUID)) {
            return;
        }

        String name = receiver.getPlainTextName().toLowerCase();
        if (message.signedContent().toLowerCase().contains(name)) {
            ping(receiver);
        }
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
