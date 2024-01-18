package org.hashdb.ms.net.nio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.event.CloseServerEvent;
import org.hashdb.ms.event.StartServerEvent;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.util.TimeCounter;
import org.hashdb.ms.util.VirtualNioEventLoopGroup;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;

/**
 * Date: 2024/1/16 19:17
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class NettyServer implements DisposableBean, AutoCloseable {
    protected final DBServerConfig serverConfig;

    protected final DBSystem dbSystem;

    protected final ClientChannelInitializer clientChannelInitializer;
    protected Channel serverChannel;
    protected final EventLoopGroup bossGroup;

    protected final EventLoopGroup workerGroup;

    {
        // 使用虚拟+nio eventloop
        bossGroup = new VirtualNioEventLoopGroup(1);
        workerGroup = new VirtualNioEventLoopGroup();
        // 使用虚拟线程, 但是放弃了netty的优化
//        bossGroup = new NioEventLoopGroup(1, AsyncService.virtualFactory("v-b-nio-"));
//        workerGroup = new NioEventLoopGroup(AsyncService.virtualFactory("v-w-nio-"));
    }

    public NettyServer(DBServerConfig serverConfig, DBSystem dbSystem, ClientChannelInitializer clientChannelInitializer) {
        this.serverConfig = serverConfig;
        this.dbSystem = dbSystem;
        this.clientChannelInitializer = clientChannelInitializer;
    }

    @EventListener(StartServerEvent.class)
    public void run() {
        var costTime = TimeCounter.costTime(() -> {
            var bootstrap = new ServerBootstrap();
            serverChannel = bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(clientChannelInitializer)
                    .bind(serverConfig.getPort())
                    .channel();
        });
        log.info("server is running at [tcp: {}] cost {}ms", serverConfig.getPort(), costTime);
    }


    @Override
    @EventListener(CloseServerEvent.class)
    public void close() throws Exception {
        log.info("server closing");
        var costTime = TimeCounter.costTime(() -> {
            var channelFuture = serverChannel.close();
            var bossFuture = bossGroup.shutdownGracefully();
            var workerFuture = workerGroup.shutdownGracefully();
            try {
                channelFuture.sync();
                bossFuture.sync();
                workerFuture.sync();
            } catch (InterruptedException e) {
                log.warn("interrupted", e);
            }
        });
        log.info("server closed. cost {}ms", costTime);
    }

    @Override
    public void destroy() throws Exception {

    }
}
