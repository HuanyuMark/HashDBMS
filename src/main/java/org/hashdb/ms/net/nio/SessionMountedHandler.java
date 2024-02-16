package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.MaxConnectionException;
import org.hashdb.ms.net.exception.ReconnectErrorException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.util.AsyncService;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Date: 2024/1/17 12:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@ChannelHandler.Sharable
public class SessionMountedHandler extends ChannelDuplexHandler implements Closeable, NamedChannelHandler {
    private volatile boolean closed;
    private final ConcurrentMap<Long, TransientConnectionSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object unknownMsg, ChannelPromise promise) throws Exception {
        if (unknownMsg instanceof Message<?> msg) {
            msg.session(ctx.channel().attr(TransientConnectionSession.KEY).get());
        }
        ctx.write(unknownMsg, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object m) {
        var attr = ctx.channel().attr(TransientConnectionSession.KEY);
        var session = attr.get();
        var msg = (Message<?>) m;
        if (session != null) {
            msg.session(session);
            ctx.fireChannelRead(m);
            return;
        }
        // 如果是重连消息, 则body就是上次连接所使用的session id
        if (m instanceof ReconnectMessage reconnectMsg) {
            loadInactiveSession(ctx, attr, reconnectMsg);
            return;
        }
        // TODO: 2024/2/15 必须要在auth后才能切换Session
        if (m instanceof SessionSwitchingMessage switchingMsg) {
            if (switchSession(ctx, attr, switchingMsg) == null) {
                return;
            }
            ctx.fireChannelRead(m);
            return;
        }
        var switchingSession = switchSession(ctx, attr, SessionSwitchingMessage.DEFAULT);
        if (switchingSession == null) {
            return;
        }
        msg.session(switchingSession);
        ctx.fireChannelRead(m);
    }

    private void doCloseSession(TransientConnectionSession session) {
        sessionMap.remove(session.id());
        session.stopInactive();
    }

    private @Nullable TransientConnectionSession switchSession(ChannelHandlerContext ctx, Attribute<TransientConnectionSession> attr, SessionSwitchingMessage msg) {
        var currentSession = attr.get();
        var meta = msg.body();
        if (meta.sessionClass().isInstance(currentSession)) {
            return currentSession;
        }
        TransientConnectionSession newSession;
        try {
            newSession = switch (meta) {
                case BUSINESS -> new BusinessConnectionSession() {
                    @Override
                    protected void onClose(TransientConnectionSession session) {
                        doCloseSession(session);
                    }
                };
                case MANAGEMENT -> new ManageConnectionSession(currentSession.id()) {
                    @Override
                    protected void onClose(TransientConnectionSession session) {
                        doCloseSession(session);
                    }
                };
            };
        } catch (MaxConnectionException e) {
            attr.set(BusinessConnectionSession.DEFAULT);
            ctx.write(new CloseMessage(0, e));
            ctx.close();
            return null;
        }
        newSession.onChannelActive(ctx.channel());
        log.info("store new session {}", newSession);
        sessionMap.put(newSession.id(), newSession);
        attr.set(newSession);
        msg.session(newSession);
        return newSession;
    }

    private void loadInactiveSession(ChannelHandlerContext ctx, Attribute<TransientConnectionSession> sessionAttribute, ReconnectMessage reconnectMessage) {
        log.info("load inactive session");
        var session = sessionMap.get(reconnectMessage.body());
        if (session == null) {
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
        session.onChannelChange(ctx.channel());
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
        var session = ctx.channel().attr(TransientConnectionSession.KEY).get();
        if (session == null) {
            log.info("ex: {} no session channel closed: {}", cause.getMessage(), ctx.channel());
            ctx.channel().close();
            return;
        }
        session.startInactive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        var session = ctx.channel().attr(TransientConnectionSession.KEY).get();
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
}
