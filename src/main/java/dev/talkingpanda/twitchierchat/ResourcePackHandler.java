package dev.talkingpanda.twitchierchat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import io.netty.handler.codec.http.*;
import offsetmonkey538.meshlib.api.HttpHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class ResourcePackHandler implements HttpHandler {

    @Override
    public void handleRequest(@NotNull ChannelHandlerContext ctx, @NotNull FullHttpRequest request) {
        final File resourcePackFile = new File("./config/twitchierchat/emotes.zip");
        if (!resourcePackFile.exists())
            HttpHandler.sendError(ctx, HttpResponseStatus.NOT_FOUND, "File not found");
        try {
            byte[] fileBytes = Files.readAllBytes(resourcePackFile.toPath());
            ByteBuf content = Unpooled.copiedBuffer(fileBytes);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/zip");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } catch (IOException e) {
            HttpHandler.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }
}

