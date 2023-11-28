package org.hashdb.ms.communication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.CI;
import org.hashdb.ms.util.Runners;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2023/11/20 20:10
 * 使用 Socket协议与其它进程取得通信
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
//@Component
@RequiredArgsConstructor
public class ConnectionServer implements InitializingBean {
    private ServerSocketChannel serverChannel;

    private final Map<SocketAddress, SocketChannel> clientConnections = new ConcurrentHashMap<>();

    private final DBServerConfig dbServerConfig;

    public CompletableFuture<Void> start(Integer port) {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(port));
            // 等待新连接
            serverChannel.configureBlocking(true);
            log.info("DBMS Server is listening at port: " + port);
            return start0();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取客户端链接
     */
    protected CompletableFuture<Void> start0() {
        return AsyncService.submit(() -> Runners.everlasting(() -> {
            try {
                var clientConnection = serverChannel.accept();
//                clientConnections.put(clientConnection.getRemoteAddress(), clientConnection);
                AsyncService.submit(() -> {
                    while (clientConnection.isConnected()) {
                        ByteBuffer buffer = ByteBuffer.allocate(100 * 1024);
                        try {
                            int unRead = clientConnection.read(buffer);
//                            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(buffer.array()));
//                            Object o = inputStream.readObject();
                            byte[] bytes = Arrays.copyOf(buffer.array(), buffer.position());
                            String o = new String(bytes);
                            if("close".equals(o)) {
                                clientConnection.close();
                                clientConnection.socket().close();
                            }
                            System.out.println("unRead" + unRead + " obj: " + o);
                            clientConnection.write(ByteBuffer.wrap("ok".getBytes()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public void stop() throws IOException {
        if (serverChannel == null) {
            return;
        }
        serverChannel.close();
        serverChannel = null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start(dbServerConfig.getPort());
    }
}
