package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;


public class Replies {
    private static final ReplyQueue<Pair<ServerPlayer, Component>> queue = new ReplyQueue<>();

    private static long addToQueue(ServerPlayer player, Component contents) {
        return queue.add(new Pair<>(player, contents));
    }

    public static OutgoingChatMessage handleOutgoingMessage(OutgoingChatMessage message, ChatType.Bound bound) {
        if (!(message instanceof OutgoingChatMessage.Player)) return message;

        Component messageComponent = message.content();
        PlayerChatMessage playerMessage = ((OutgoingChatMessage.Player) message).message();

        var chatType = bound.chatType();

        //EMOTE_COMMAND refers to /me.
        if (chatType.is(ChatType.CHAT) || chatType.is(ChatType.SAY_COMMAND) || chatType.is(ChatType.EMOTE_COMMAND)) {

            long id = addToQueue(TwitchierChat.minecraftServer.getPlayerList().getPlayer(playerMessage.sender()), messageComponent);

            return OutgoingChatMessage.create(playerMessage.withUnsignedContent(addReplyButton(bound.name().getString(), messageComponent, id)));
        }

        return message;
    }


    private static MutableComponent addReplyButton(String userName, Component message, long id) {
        return message.copy().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Reply to ").append(userName))).withClickEvent(new ClickEvent.SuggestCommand("/reply " + id + " ")));
    }

    private static @Nullable Pair<ServerPlayer, Component> getReplyFor(long id) {
        return queue.get(id);
    }

    private static Component formatReplyText(Component original) {
        MutableComponent result = original.plainCopy();

        for (Component sibling : original.getSiblings()) {
            result.append(sibling.copy().setStyle(Style.EMPTY));
        }

        return result;
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("reply").requires(CommandSourceStack::isPlayer).then(Commands.argument("messageId", IntegerArgumentType.integer()).then(Commands.argument("message", MessageArgument.message()).executes(context -> {
                    MessageArgument.resolveChatMessage(context, "message", message -> {
                        long id = IntegerArgumentType.getInteger(context, "messageId");

                        CommandSourceStack source = context.getSource();
                        ServerPlayer sourcePlayer = source.getPlayer();
                        if (sourcePlayer == null) {
                            return;
                        }
                        PlayerList playerList = source.getServer().getPlayerList();
                        var msg = Replies.getReplyFor(id);
                        if (msg == null) {
                            context.getSource().sendFailure(Component.literal("Failed to get the target message, the message maybe too old to reply to").withColor(CommonColors.RED));
                            return;
                        }

                        Component replyText = Component.literal("Replying to " + msg.getFirst().getPlainTextName() + ": ").append(formatReplyText(msg.getSecond())).withStyle(Style.EMPTY.withShadowColor(CommonColors.BLACK).withColor(ChatFormatting.GRAY));
                        playerList.broadcastSystemMessage(replyText, false);
                        playerList.broadcastChatMessage(message, source, ChatType.bind(ChatType.CHAT, source));

                        UUID uuid = msg.getFirst().getUUID();
                        UUID sourceUUID = sourcePlayer.getUUID();

                        if (Config.shouldPing(uuid, sourceUUID, true) && !uuid.equals(sourceUUID)) {
                            Pings.ping(msg.getFirst());
                        }
                    });
                    return 1;
                }
        ))));

    }
}
