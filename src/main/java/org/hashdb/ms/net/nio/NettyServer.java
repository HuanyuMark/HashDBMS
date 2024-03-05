package org.hashdb.ms.net.nio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.util.TimeCounter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Date: 2024/1/16 19:17
 *
 * @author Huanyu Mark
 */
@Slf4j
@Component
public class NettyServer implements DisposableBean, AutoCloseable {
    protected final DBServerConfig serverConfig;

    protected final DBSystem dbSystem;

    protected final ClientChannelInitializer clientChannelInitializer;
    protected Channel serverChannel;
    protected final EventLoopGroup bossGroup;

    protected final EventLoopGroup workerGroup;

    private volatile boolean closed;

    {
        // acceptor
        bossGroup = new NioEventLoopGroup(Math.max(Runtime.getRuntime().availableProcessors() - 1 >> 1, 1));
        // worker
        workerGroup = new NioEventLoopGroup();
    }

    public NettyServer(DBServerConfig serverConfig, DBSystem dbSystem, ClientChannelInitializer clientChannelInitializer) {
        this.serverConfig = serverConfig;
        this.dbSystem = dbSystem;
        this.clientChannelInitializer = clientChannelInitializer;
    }

    @EventListener(ApplicationContext.class)
    public void run() {
        // 先正常启动
        var startTime = TimeCounter.start();
        var server = new ServerBootstrap();
        var future = server.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, serverConfig.getMaxConnections())
                .childHandler(clientChannelInitializer)
                .bind(serverConfig.getPort());
        serverChannel = future.channel();
        future.addListener(res -> {
            if (res.isSuccess()) {
                log.info("server is running at [tcp: {}] cost {}ms", serverConfig.getPort(), startTime);
                return;
            }
            throw Exit.error(log, "server start failed", res.cause());
        });
        // 启动后, 如果其次启动属于故障恢复, 则等待其它结点的服务发现机制(poll ping)
        // 这个结点被ping通并身份验证后, 执行其它结点给的指令, 执行故障恢复启动的逻辑
    }


    @Override
    public void close() {
        if (closed) {
            return;
        }
        log.info("server closing");
        var timeCounter = TimeCounter.start();
        clientChannelInitializer.close();
        var channelFuture = serverChannel.close();
        var bossFuture = bossGroup.shutdownGracefully();
        var workerFuture = workerGroup.shutdownGracefully();
        try {
            channelFuture.sync();
            bossFuture.sync();
            workerFuture.sync();
            closed = true;
            log.info("server closed. cost {}ms", timeCounter);
        } catch (InterruptedException e) {
            log.error("server closing process is interrupted", e);
        }
    }

    @Override
    // 关闭服务器
    public void destroy() {
        close();
    }
}
