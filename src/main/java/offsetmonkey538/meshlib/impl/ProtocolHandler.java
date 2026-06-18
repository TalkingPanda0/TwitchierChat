package offsetmonkey538.meshlib.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import offsetmonkey538.meshlib.api.HttpHandlerRegistry;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static offsetmonkey538.meshlib.api.HttpHandler.sendError;

public class ProtocolHandler extends ChannelInboundHandlerAdapter {
    public static final String MOD_ID = "mesh-lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object request) {
        if (!(request instanceof ByteBuf buf)) {
            LOGGER.warn("Received request '{}' that wasn't a ByteBuf", request);
            return;
        }

        final StringBuilder firstLine = new StringBuilder();
        for (int i = 0; i < buf.readableBytes(); i++) {
            char currentChar = (char) buf.getByte(i);
            firstLine.append(currentChar);
            if (currentChar == '\n') break;
        }
        final boolean isHttp = firstLine.toString().contains("HTTP");


        if (isHttp) {
            final String uri = firstLine.toString().split(" ")[1];
            if (uri.equals("/")) {
                LOGGER.debug("Request was made to root domain! Passing on...");
                forward(ctx, request);
                return;
            }

            final String handlerId = uri.split("/")[1];
            if (!HttpHandlerRegistry.INSTANCE.has(handlerId)) {
                LOGGER.debug("Handler with id '{}' not registered! Passing on...", handlerId);
                forward(ctx, request);
                return;
            }

            final ChannelPipeline pipeline = ctx.pipeline();
            pipeline.addAfter(MOD_ID, MOD_ID + "/codec", new HttpServerCodec());
            pipeline.addAfter(MOD_ID + "/codec", MOD_ID + "/aggregator", new HttpObjectAggregator(65536));
            pipeline.addAfter(MOD_ID + "/aggregator", MOD_ID + "/handler", new MainHttpHandler());
        }

        forward(ctx, request);
    }

    private void forward(ChannelHandlerContext ctx, Object request) {
        ctx.pipeline().remove(MOD_ID);
        ctx.fireChannelRead(request);
    }
}

class MainHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        final String handlerId = request.uri().split("/")[1];

        HttpHandlerRegistry.INSTANCE.get(handlerId).handleRequest(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final Logger LOGGER = LoggerFactory.getLogger("mesh-lib");
        LOGGER.error("Failed to handle request", cause);

        if (!ctx.channel().isActive()) return;
        sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, cause.getMessage());
    }
}