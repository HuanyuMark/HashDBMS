package org.hashdb.ms.net.nio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.event.ApplicationContextLoadedEvent;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.TimeCounter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Date: 2024/1/16 19:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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
        int acceptor = Runtime.getRuntime().availableProcessors() - 1 >> 1;
        bossGroup = new NioEventLoopGroup(acceptor <= 0 ? acceptor : 1);
        workerGroup = new NioEventLoopGroup();
    }

    public NettyServer(DBServerConfig serverConfig, DBSystem dbSystem, ClientChannelInitializer clientChannelInitializer) {
        this.serverConfig = serverConfig;
        this.dbSystem = dbSystem;
        this.clientChannelInitializer = clientChannelInitializer;
    }

    @EventListener(ApplicationContextLoadedEvent.class)
    public void run() {
        JsonService.loadConfig();
        var startTime = TimeCounter.start();
        var bootstrap = new ServerBootstrap();
        ChannelFuture channelFuture = bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(clientChannelInitializer)
                .bind(serverConfig.getPort());
        serverChannel = channelFuture.channel();
        channelFuture.addListener(f -> {
            if (f.isSuccess()) {
                log.info("server is running at [tcp: {}] cost {}ms", serverConfig.getPort(), startTime.stop());
                return;
            }
            log.error("server start failed", f.cause());
            System.exit(1);
        });
    }


    @Override
    public void close() throws Exception {
        if (closed) {
            return;
        }
        log.info("server closing");
        var costTime = TimeCounter.costTime(() -> {
            try {
                clientChannelInitializer.close();
                serverChannel.close().sync();
                var bossFuture = bossGroup.shutdownGracefully();
                var workerFuture = workerGroup.shutdownGracefully();
                bossFuture.sync();
                workerFuture.sync();
            } catch (InterruptedException e) {
                log.warn("interrupted", e);
            }
        });
        closed = true;
        log.info("server closed. cost {}ms", costTime);
    }

    @Override
    // 关闭服务器
    public void destroy() throws Exception {
        close();
    }
}
