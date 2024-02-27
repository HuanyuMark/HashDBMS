package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.NamedChannelHandler;
import org.hashdb.ms.net.nio.msg.v1.ActAppCommandMessage;
import org.hashdb.ms.net.nio.msg.v1.FlyweightMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.util.AsyncService;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Date: 2024/2/19 12:00
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class ProtocolCodec extends MessageToMessageCodec<ByteBuf, Message<?>> implements NamedChannelHandler {

    private Channel channel;

    private Protocol protocol;

    private static LoggingHandler warnHandler;

    // 协议解析器, 需要配合帧解析器运作, 根据body长度进行消息解析, 所以如果messageCodec所定义的长度字段与上述的frameDecoder
    // 的规定不同, 则粘包粘包现象会存在, 且有frameDecoder可能会抛解析异常
    public ProtocolCodec(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        channel = ctx.channel();
        channel.pipeline().addBefore(handlerName(), NamedChannelHandler.handlerName(LengthFieldBasedFrameDecoder.class), protocol.codec().frameDecoder());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        ctx.channel().pipeline().remove(LengthFieldBasedFrameDecoder.class);
        channel = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message<?> msg, List<Object> out) {
        // 命令响应体的包体体力变动较大, 不确定要消耗多少时间在编码上,为了不阻塞IO线程,所以这里采用异步处理
        if (msg instanceof FlyweightMessage) {
            var cacheBuf = protocol.sharedMessageByteBufMap().computeIfAbsent(msg, m -> {
                var b = protocol.codec().encode(ctx, m);
                b.discardReadBytes();
                return b;
            });
            out.add(cacheBuf);
            return;
        }
        if (msg instanceof ActAppCommandMessage) {
            AsyncService.start(() -> protocol.codec().encode(ctx, msg)).handleAsync((buf, e) -> {
                if (e == null) {
                    ctx.writeAndFlush(buf);
                    return buf;
                }
                warnCodecError("encode error:", e);
                return e;
            }, AsyncService.service());
        } else {
            out.add(protocol.codec().encode(ctx, msg));
        }
    }

    private static void warnCodecError(String msg, Throwable e) {
        if (warnHandler == null) {
            warnHandler = new LoggingHandler("protocol.codec() error logger", LogLevel.WARN);
        }
        log.warn(msg, e);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        try {
            out.add(protocol.codec().decode(ctx, buf));
        } catch (Exception e) {
            warnCodecError("decode error:", e);
            warnHandler.channelRead(ctx, buf);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            super.write(ctx, msg, promise);
        } catch (Exception e) {
            warnCodecError("encode error:", e);
            warnHandler.write(ctx, msg, promise);
        }
    }

    public void setProtocol(Protocol protocol) {
        if (protocol == this.protocol) {
            return;
        }
        if (channel == null) {
            throw new IllegalStateException("handler should be added");
        }
        this.protocol = protocol;
        try {
            channel.pipeline().replace(LengthFieldBasedFrameDecoder.class, NamedChannelHandler.handlerName(LengthFieldBasedFrameDecoder.class), protocol.codec().frameDecoder());
        } catch (NoSuchElementException e) {
            channel.pipeline().addBefore(handlerName(), NamedChannelHandler.handlerName(LengthFieldBasedFrameDecoder.class), protocol.codec().frameDecoder());
        }
    }
}
