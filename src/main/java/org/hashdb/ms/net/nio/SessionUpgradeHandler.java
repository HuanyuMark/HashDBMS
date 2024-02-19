package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.exception.IllegalUpgradeSessionException;
import org.hashdb.ms.net.exception.MaxConnectionException;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.msg.v1.SessionUpgradeMessage;

import java.util.Map;

/**
 * Date: 2024/2/18 11:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Sharable
public class SessionUpgradeHandler extends SimpleChannelInboundHandler<SessionUpgradeMessage> implements NamedChannelHandler {
    private final Map<Long, BaseConnectionSession> sessionMap;

    public SessionUpgradeHandler(Map<Long, BaseConnectionSession> sessionMap) {
        this.sessionMap = sessionMap;
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
            newSession = switch (meta) {
                case MANAGEMENT -> new ManageConnectionSession(currentSession) {
                    @Override
                    public void onClose(TransientConnectionSession session) {
                        sessionMap.remove(session.id());
                        session.stopInactive();
                    }
                };
                default -> throw new IllegalUpgradeSessionException(currentSession.getSessionMeta(), meta);
            };
        } catch (MaxConnectionException e) {
            attr.set(BusinessConnectionSession.DEFAULT);
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
