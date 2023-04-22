package com.scrapingconsole.gateway.api.configuration;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Custom logging handler overriding the default Netty LoggingHandler class.
 */
public class CustomLoggingHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomLoggingHandler.class);

    /**
     * Overrides the read in the channel.
     *
     * @param ctx The context.
     * @param msg The message that is read.
     *
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (this.ignoreMessage(msg)) {
            ctx.fireChannelRead(msg);

            return;
        }

        LOGGER.info("READ {}", kv("Message", this.format(ctx, msg)));

        ctx.fireChannelRead(msg);
    }

    /**
     * Overrides the channel write.
     *
     * @param ctx     The context.
     * @param msg     The message.
     * @param promise The promise.
     *
     * @throws Exception
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (this.ignoreMessage(msg)) {
            ctx.write(msg);

            return;
        }

        LOGGER.info("WRITE {}", kv("Message", this.format(ctx, msg)));

        ctx.write(msg, promise);
    }

    /**
     * Internal formatting. Handles ByteBuf and ByteBufHolder. All others are just sent to the super method.
     *
     * @param ctx The context.
     * @param arg The object that is to be formatted.
     *
     * @return the formatted string to be logged.
     */
    protected String format(ChannelHandlerContext ctx, Object arg) {
        if (arg instanceof ByteBuf) {
            ByteBuf msg = (ByteBuf) arg;
            String formatted = this.formatByteBuf(ctx, msg);

            return formatted;
        }

        if (arg instanceof ByteBufHolder) {
            ByteBufHolder msg = (ByteBufHolder) arg;
            String formatted = this.formatByteBufHolder(ctx, msg);

            return formatted;
        }

        // Fallback to super
        return this.formatSimple(ctx, arg);
    }

    /**
     * Simple formatting for a ByteBufHolder.
     *
     * @param ctx The context
     * @param msg The ByteBufHolder, contains the ByteBuf.
     *
     * @return the formatted message.
     */
    private String formatByteBufHolder(ChannelHandlerContext ctx, ByteBufHolder msg) {
        String chStr = ctx.channel().toString();
        String msgStr = msg.toString();
        ByteBuf content = msg.content();
        int length = content.readableBytes();

        if (length == 0) {
            return new StringBuilder(chStr.length() + 4)
                    .append(chStr)
                    .append(": 0B")
                    .toString();
        }

        String message = content.toString(StandardCharsets.UTF_8);
        int outputLength = chStr.length() + 2 + 10 + 2 + message.length();
        StringBuilder buf = new StringBuilder(outputLength)
                .append(chStr)
                .append(": ")
                .append(length)
                .append("B ")
                .append(msgStr);

        return buf.toString();
    }

    /**
     * Simple formatting for a ByteBuf.
     *
     * @param ctx The context
     * @param msg The ByteBuf.
     *
     * @return the formatted message.
     */
    private String formatByteBuf(ChannelHandlerContext ctx, ByteBuf msg) {
        String chStr = ctx.channel().toString();
        int length = msg.readableBytes();
        if (length == 0) {
            return new StringBuilder(chStr.length() + 4)
                    .append(chStr)
                    .append(": 0B")
                    .toString();
        } else {
            String message = msg.toString(StandardCharsets.UTF_8);
            int outputLength = chStr.length() + 2 + 10 + 1 + 1 + message.length();

            StringBuilder buf = new StringBuilder(outputLength)
                    .append(chStr)
                    .append(": ")
                    .append(length)
                    .append('B')
                    .append(' ')
                    .append(message);

            return buf.toString();
        }
    }

    /**
     * Required to blank out the user context. We don't want to log this.
     *
     * @param ctx The ChannelHandlerContext.
     * @param msg The object, typically a DefaultHttpRequest.
     *
     * @return the formatted string to log.
     */
    private static String formatSimple(ChannelHandlerContext ctx, Object msg) {
        String chStr = ctx.channel().toString();
        String msgStr = String.valueOf(msg).replaceFirst("UserContext: [\\w+=]*", "UserContext: ***************");
        StringBuilder buf = new StringBuilder(chStr.length() + 2 + msgStr.length());

        return buf.append(chStr)
                .append(": ")
                .append(msgStr).toString();
    }

    /**
     * Ignore messages from logging.
     *
     * @param msg The message.
     *
     * @return true if the message should not be logged.
     */
    private boolean ignoreMessage(Object msg) {
        // Internal class but toString() returns EmptyLastHttpContent
        if ("EmptyLastHttpContent".equals(msg.toString())) {
            return true;
        }

        return false;
    }
}
