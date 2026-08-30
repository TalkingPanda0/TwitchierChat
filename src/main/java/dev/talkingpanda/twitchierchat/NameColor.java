package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NameColor {

    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("red", 0xFF0000),
            Map.entry("blue", 0x0000FF),
            Map.entry("green", 0x008000),
            Map.entry("firebrick", 0xb22222),
            Map.entry("coral", 0xFF7F50),
            Map.entry("yellowgreen", 0x9acd32),
            Map.entry("orangered", 0xFF4500),
            Map.entry("seagreen", 0x2e8b57),
            Map.entry("goldenrod", 0xdaa520),
            Map.entry("chocolate", 0xd2691e),
            Map.entry("cadetblue", 0x5f9ea0),
            Map.entry("dodgerblue", 0x1e90ff),
            Map.entry("hotpink", 0xff69b4),
            Map.entry("blueviolet", 0x8a2be2),
            Map.entry("springgreen", 0x00ff7f)
    );


    private static @Nullable Integer getColor(String input) throws Exception {
        if (input.startsWith("#")) {
            input = input.substring(1);
            try {
                return Integer.parseInt(input, 16);
            } catch (NumberFormatException e) {
                throw new Exception("Invalid hex " + input);
            }
        }

        if (input.equalsIgnoreCase("clear")) {
            return null;
        }

        Integer color = COLORS.get(input);

        if (color == null) {
            throw new Exception("Invalid color " + input);
        }

        return color;
    }

    private static void updateNameColor(ServerPlayer player) {
        var packet = new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player);
        TwitchierChat.minecraftServer.getPlayerList().broadcastAll(packet);
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> colors = new ArrayList<>(COLORS.keySet());
        colors.add("clear");

        dispatcher.register(Commands.literal("color").requires(CommandSourceStack::isPlayer).then(Commands.argument("color", StringArgumentType.greedyString())
                .suggests((c, p) -> SharedSuggestionProvider.suggest(colors, p)).executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }

                    try {
                        String input = StringArgumentType.getString(context, "color");
                        Integer color = getColor(input);
                        if (color == null) {
                            Config.removeColor(player.getUUID());
                            context.getSource().sendSuccess(() -> Component.literal("Cleared your name color"), false);
                            updateNameColor(player);
                            return 1;
                        }

                        Config.setColor(player.getUUID(), color);
                        updateNameColor(player);
                        context.getSource().sendSuccess(() -> Component.literal("Changed your name color to ").append(Component.literal(input).withColor(color)), false);
                    } catch (Exception e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()).withColor(CommonColors.RED));
                    }
                    return 1;
                })));
    }

}
