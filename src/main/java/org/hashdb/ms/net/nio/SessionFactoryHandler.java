package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import org.hashdb.ms.net.exception.ReconnectErrorException;
import org.hashdb.ms.net.nio.msg.v1.ActMessage;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.msg.v1.ReconnectMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Date: 2024/1/17 12:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@ChannelHandler.Sharable
public class SessionFactoryHandler extends ChannelInboundHandlerAdapter {
    private final ConcurrentMap<Long, NettyConnectionSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object m) {
        var sessionAttribute = ctx.channel().attr(NettyConnectionSession.KEY);
        var existSession = sessionAttribute.get();
        if (existSession != null) {
            if (m instanceof Message<?> msg) {
                msg.session(existSession);
            }
            ctx.fireChannelRead(m);
            return;
        }
        // 如果是重连消息, 则body就是上次连接所使用的session id
        if (m instanceof ReconnectMessage reconnectMessage) {
            var session = sessionMap.get(reconnectMessage.body());
            if (session == null) {
                var err = new ErrorMessage(new ReconnectErrorException("can not reconnect: not found session"));
                ctx.writeAndFlush(err);
                return;
            }
            // 如果这个session还在被一个channel使用
            if (session.isActive()) {
                var err = new ErrorMessage(new ReconnectErrorException("can not reconnect: session is in used"));
                ctx.writeAndFlush(err);
                return;
            }
            startAuth(ctx, m, sessionAttribute, session);
            ctx.writeAndFlush(ActMessage.defaultAct(reconnectMessage.id()));
            return;
        }
        var newSession = new NettyConnectionSession(ctx.channel(), s -> sessionMap.remove(s.id()));
        sessionMap.put(newSession.id(), newSession);
        startAuth(ctx, m, sessionAttribute, newSession);
        ctx.fireChannelRead(m);
    }

    private static void startAuth(ChannelHandlerContext ctx, Object msg, Attribute<NettyConnectionSession> sessionAttribute, NettyConnectionSession session) {
        // 在SessionFactory后添加登录验证处理器
//        ctx.pipeline().addAfter("SessionFactory", "AuthorizeHandler", session.authorizeHandler());
        ctx.pipeline().addLast("AuthorizeHandler", session.authorizeHandler());
        ctx.pipeline().addLast("CommandExecuteHandler", session.commandExecuteHandler());
        sessionAttribute.set(session);
        // 便于通过Message取得session
        if (msg instanceof Message<?> m) {
            m.session(session);
        }
    }
}
