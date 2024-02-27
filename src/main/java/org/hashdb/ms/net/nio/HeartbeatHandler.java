package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.net.nio.msg.v1.ServerPingMessage;
import org.hashdb.ms.util.AsyncService;

import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2024/2/3 21:41
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
class HeartbeatHandler extends ChannelInboundHandlerAdapter implements NamedChannelHandler {

    private ScheduledFuture<?> checkHeartbeatTask;

    private int missedHeartbeatCount;

    private Channel channel;

    @Setter
    private Message<?> pingMessage;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        channel = ctx.channel();
        startHeartbeat();
        ctx.fireChannelActive();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        channel = null;
        stopHeartbeat();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        nextHeartbeat();
        if (msg instanceof ServerPingMessage ping) {
            ctx.write(ping.pong());
            return;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channel = null;
        stopHeartbeat();
    }

    private void stopHeartbeat() {
        if (checkHeartbeatTask == null) {
            return;
        }
        checkHeartbeatTask.cancel(true);
    }

    private void startHeartbeat() {
        checkHeartbeatTask = AsyncService.setTimeout(() -> {
            channel.writeAndFlush(pingMessage == null || pingMessage instanceof ServerPingMessage ? (pingMessage = new ServerPingMessage()) : pingMessage);
            if (++missedHeartbeatCount < HashDBMSApp.dbServerConfig.get().getTimeoutRetry()) {
                return;
            }
            log.info("heartbeat timeout {}", channel);
            closeChannel(channel);
        }, HashDBMSApp.dbServerConfig.get().getHeartbeatInterval());
    }

    protected void closeChannel(Channel channel) {
        channel.close();
    }

    private void nextHeartbeat() {
        missedHeartbeatCount = 0;
        checkHeartbeatTask.cancel(true);
        startHeartbeat();
    }
}
