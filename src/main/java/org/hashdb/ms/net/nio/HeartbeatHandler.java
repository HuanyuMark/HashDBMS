package org.hashdb.ms.net.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.nio.msg.v1.CloseMessage;
import org.hashdb.ms.net.nio.msg.v1.PingMessage;
import org.hashdb.ms.util.AsyncService;

import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/3 21:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@RequiredArgsConstructor
class HeartbeatHandler extends ChannelInboundHandlerAdapter implements NamedChannelHandler {

    private final TransientConnectionSession session;

    private ScheduledFuture<?> checkHeartbeatTask;

    private int missedHeartbeatCount;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        nextHeartbeat();
        ctx.fireChannelRead(msg);
    }

    public void stopHeartbeat() {
        if (checkHeartbeatTask == null) {
            return;
        }
        checkHeartbeatTask.cancel(true);
    }

    public void startHeartbeat() {
        checkHeartbeatTask = AsyncService.setTimeout(() -> {
            session.channel().writeAndFlush(new PingMessage());
            if (++missedHeartbeatCount < ConnectionSession.dbServerConfig.get().getTimeoutRetry()) {
                return;
            }
            log.info("heartbeat timeout {}", session);
            session.close(new CloseMessage(0, "heartbeat timeout"));
        }, ConnectionSession.dbServerConfig.get().getHeartbeatInterval());
    }

    private void nextHeartbeat() {
        missedHeartbeatCount = 0;
        checkHeartbeatTask.cancel(true);
        startHeartbeat();
    }
}
