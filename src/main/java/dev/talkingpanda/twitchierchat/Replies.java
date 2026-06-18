package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.CommonColors;


public class Replies {
    private static final ReplyQueue<Pair<String, Component>> queue = new ReplyQueue<>();

    private static long addToQueue(String userName, Component contents) {
        return queue.add(new Pair<>(userName, contents));
    }

    public static OutgoingChatMessage handleOutgoingMessage(OutgoingChatMessage message, ChatType.Bound bound) {
        if (!(message instanceof OutgoingChatMessage.Player)) return message;

        String userName = bound.name().getString();
        Component messageComponent = message.content();
        PlayerChatMessage playerMessage = ((OutgoingChatMessage.Player) message).message();

        var chatType = bound.chatType();

        //EMOTE_COMMAND refers to /me.
        if (chatType.is(ChatType.CHAT) || chatType.is(ChatType.SAY_COMMAND) || chatType.is(ChatType.EMOTE_COMMAND)) {
            long id = addToQueue(userName, messageComponent);

            return OutgoingChatMessage.create(playerMessage.withUnsignedContent(addReplyButton(userName, messageComponent, id)));
        }

        return message;
    }


    private static MutableComponent addReplyButton(String userName, Component message, long id) {
        return message.copy().setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Reply to ").append(userName))).withClickEvent(new ClickEvent.SuggestCommand("/reply " + id + " ")));
    }

    private static Pair<String, Component> getReplyFor(long id) {
        return queue.get(id);
    }

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("reply").requires(CommandSourceStack::isPlayer).then(Commands.argument("messageId", IntegerArgumentType.integer()).then(Commands.argument("message", MessageArgument.message()).executes(context -> {
                    MessageArgument.resolveChatMessage(context, "message", message -> {
                        long id = IntegerArgumentType.getInteger(context, "messageId");

                        CommandSourceStack source = context.getSource();
                        PlayerList playerList = source.getServer().getPlayerList();
                        var msg = Replies.getReplyFor(id);
                        playerList.broadcastSystemMessage(Component.literal("Replying to " + msg.getFirst() + ": ").append(msg.getSecond()).withColor(CommonColors.GRAY), false);
                        playerList.broadcastChatMessage(message, source, ChatType.bind(ChatType.CHAT, source));
                    });
                    return 1;
                }
        ))));

    }
}
