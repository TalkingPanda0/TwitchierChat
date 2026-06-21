package offsetmonkey538.meshlib.api;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static offsetmonkey538.meshlib.impl.ProtocolHandler.LOGGER;

@FunctionalInterface
public interface HttpHandler {

    void handleRequest(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request);

    static void sendError(@NotNull ChannelHandlerContext ctx, @NotNull HttpResponseStatus status) {
        sendError(ctx, status, null);
    }

    static void sendError(@NotNull ChannelHandlerContext ctx, @NotNull HttpResponseStatus status, @Nullable String reason) {
        final String message = String.format("Failure: %s\r\n%s",status, (reason == null || reason.isBlank() ? "" : "Reason: " + reason + "\r\n"));

        LOGGER.error(message);

        final ByteBuf byteBuf = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, byteBuf);

        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
