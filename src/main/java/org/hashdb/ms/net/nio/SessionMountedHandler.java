package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.IllegalUpgradeSessionException;
import org.hashdb.ms.net.exception.MaxConnectionException;
import org.hashdb.ms.net.exception.ReconnectErrorException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.util.AsyncService;

import java.io.Closeable;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2024/1/17 12:22
 *
 * @author Huanyu Mark
 */
@Slf4j
@ChannelHandler.Sharable
public class SessionMountedHandler extends ChannelDuplexHandler implements Closeable, NamedChannelHandler {
    private volatile boolean closed;
    private final Map<Integer, BaseConnectionSession> sessionMap = new ConcurrentHashMap<>();
    private SessionUpgradeHandler upgradeHandler;

    public SessionMountedHandler() {
    }

    public SessionUpgradeHandler upgradeHandler() {
        return upgradeHandler == null ? (upgradeHandler = new SessionUpgradeHandler()) : upgradeHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        var attr = ctx.channel().attr(BaseConnectionSession.KEY);
        var session = attr.get();
        if (session != null) {
            ctx.fireChannelRead(msg);
            return;
        }
        // 如果是重连消息, 则body就是上次连接所使用的session id
        if (msg instanceof ReconnectMessage reconnectMsg) {
            loadInactiveSession(ctx, attr, reconnectMsg);
        } else {
            mountNewSession(ctx, attr, msg);
        }
    }

    private void mountNewSession(ChannelHandlerContext ctx, Attribute<BaseConnectionSession> attr, Object msg) {
        BusinessConnectionSession newSession;
        try {
            newSession = new BusinessConnectionSession() {
                @Override
                public void onClose(TransientConnectionSession session) {
                    sessionMap.remove(session.id());
                    session.stopInactive();
                }
            };
        } catch (MaxConnectionException e) {
            attr.set(BusinessConnectionSession.getDefaultSession());
            ctx.write(new CloseMessage(0, e));
            ctx.close();
            return;
        }
        attr.set(newSession);
        newSession.changeChannel(ctx.channel());
        ctx.fireChannelRead(msg);
    }

    private void loadInactiveSession(ChannelHandlerContext ctx, Attribute<BaseConnectionSession> sessionAttribute, ReconnectMessage reconnectMessage) {
        log.info("load inactive session");
        var session = sessionMap.get(reconnectMessage.body());
        if (session == null) {
            // TODO: 2024/2/18 这些固定的Message可以抽离成单例的ButeBuf来发送
            ctx.write(new ErrorMessage(reconnectMessage, new ReconnectErrorException("can not reconnect: not found session")));
            return;
        }
        if (session.username() == null) {
            ctx.write(new ErrorMessage(reconnectMessage, new ReconnectErrorException("can not reconnect: No Authentication")));
            return;
        }
        // 如果这个session还在被一个channel使用
        if (session.isActive()) {
            ctx.write(new ErrorMessage(reconnectMessage, new ReconnectErrorException("can not reconnect: session is in used")));
            return;
        }
        sessionAttribute.set(session);
        session.stopInactive();
        // 更换连接
        session.changeChannel(ctx.channel());
        ctx.write(ActMessage.act(reconnectMessage.id()));
        // 通知当前session状态
        ctx.write(new SessionStateMessage(session));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!(cause instanceof SocketException)) {
            log.error("can not handle other exception: ", cause);
            ctx.fireExceptionCaught(cause);
            return;
        }
        var session = ctx.channel().attr(BaseConnectionSession.KEY).get();
        if (session == null) {
            log.info("ex: {} no session channel closed: {}", cause.getMessage(), ctx.channel());
            ctx.channel().close();
            return;
        }
        session.startInactive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var session = ctx.channel().attr(BaseConnectionSession.KEY).get();
        if (session == null) {
            ctx.channel().close();
            return;
        }
        log.info("no session channel closed: {}", ctx.channel());
        session.startInactive();
        ctx.fireChannelInactive();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        sessionMap.values().stream().map(s -> AsyncService.start(() -> s.close())).forEach(CompletableFuture::join);
        closed = true;
    }

    public class SessionUpgradeHandler extends SimpleChannelInboundHandler<SessionUpgradeMessage> implements NamedChannelHandler {

        private SessionUpgradeHandler() {
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, SessionUpgradeMessage msg) {
            var attr = ctx.channel().attr(BaseConnectionSession.KEY);
            var currentSession = attr.get();
            var meta = msg.body();
            if (meta.sessionClass() == currentSession.getClass()) {
                return;
            }
            BaseConnectionSession newSession;
            try {
                newSession = meta.upgradeFrom(currentSession);
            } catch (MaxConnectionException e) {
                attr.set(BusinessConnectionSession.getDefaultSession());
                ctx.write(new CloseMessage(0, e));
                ctx.close();
                return;
            } catch (IllegalUpgradeSessionException e) {
                ctx.write(e.msg(msg.id()));
                return;
            }
            log.info("upgrade to session {}", newSession);
            sessionMap.put(newSession.id(), newSession);
            attr.set(newSession);
        }
    }
}
