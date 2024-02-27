package org.hashdb.ms.net.nio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.exception.IllegalMessageException;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.msg.v1.MessageMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2024/1/18 18:36
 *
 * @author huanyuMake-pecdle
 */
public interface MessageCodec {

    /**
     * 仅在开发时调用, 用以自定义检查代码
     * 该方法会在实例化时被调用一次
     */
    default void checkCodecImpls(Enum<?>[] codecImpls) {
        DBServerConfig.RunMode.DEVELOPMENT.run(() -> {
            var messageMeta = MessageMeta.class.getEnumConstants();
            if (messageMeta.length != codecImpls.length) {
                throw new DBSystemException(getClass().getSimpleName() + ": codec count should match the count" +
                        " of enum constants " + MessageMeta.class);
            }
            for (int i = 0; i < messageMeta.length; i++) {
                if (messageMeta[i].name().equals(codecImpls[i].name())) {
                    continue;
                }
                throw new DBSystemException(codecImpls[i].getClass().getSimpleName() + "[" + i + "]=" + codecImpls[i] + " is not equal to " + "messageMeta[" + i + "]=" + messageMeta[i]);
            }
        });
    }

    @Nullable Message<?> decode(ChannelHandlerContext ctx, ByteBuf in);

    default @NotNull ByteBuf encode(ChannelHandlerContext ctx, Message<?> msg) {
        return encode(ctx.alloc().buffer(), msg);
    }

    @NotNull ByteBuf encode(ByteBuf buf, Message<?> msg);

    LengthFieldBasedFrameDecoder frameDecoder();

    @Nullable
    static MessageMeta resolveMessageMeta(ChannelHandlerContext ctx, int messageMetaKey) {
        MessageMeta messageType;
        try {
            messageType = MessageMeta.resolve(messageMetaKey);
        } catch (IllegalMessageException e) {
            ctx.writeAndFlush(e.msg(0));
            return null;
        }
        return messageType;
    }
}
