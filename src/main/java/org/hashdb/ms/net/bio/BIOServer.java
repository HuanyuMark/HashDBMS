package org.hashdb.ms.net.bio;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.DBServer;
import org.hashdb.ms.net.bio.client.CloseMessage;
import org.hashdb.ms.support.StaticScanIgnore;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Runners;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2024/1/15 14:52
 * 使用传统的BIO网络模型的服务器
 *
 * @author Huanyu Mark
 */
@Slf4j
//@Component
@Deprecated
@StaticScanIgnore
public class BIOServer extends DBServer {
    protected final List<ConnectionSession> connectionSessionList = new LinkedList<>();
    private ServerSocketChannel serverChannel;

    public BIOServer(DBServerConfig serverConfig, DBSystem system) {
        super(serverConfig, system);
    }

    @Override
    protected void doStart(ServerSocketChannel serverChannel) throws IOException {
        this.serverChannel = serverChannel;
        while (true) {
            // 接收新链接
            try {
                var connection = serverChannel.accept();          //这里的Connection是连接
                connectionSessionList.add(new BIOConnectionSession(connection));
            } catch (AsynchronousCloseException ignore) {

            }
        }
    }

    @Override
    protected void serverChannelOptimizer(ServerSocketChannel serverSocketChannel) throws IOException {
        serverSocketChannel.configureBlocking(true);
    }

    @Override
    public void destroy() throws Exception {
        serverChannel.close();
    }

    @Contract(" -> new")
    private @NotNull CompletableFuture<?> test() {
        return AsyncService.start(() -> Runners.everlasting(() -> {
            try {
                var con = serverChannel.accept();

                ByteBuffer buffer = ByteBuffer.allocate(1024);
                AsyncService.start(() -> {
                    try (
                            BIOConnectionSession session = new BIOConnectionSession(con);
                    ) {
                        while (con.isConnected()) {
                            try {
                                int unRead = con.read(buffer);
//                            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(buffer.array()));
//                            Object o = inputStream.readObject();
                                if (unRead == -1) {
                                    continue;
                                }
                                byte[] bytes = Arrays.copyOf(buffer.array(), buffer.position());
                                buffer.clear();
                                String o = new String(bytes);
                                System.out.println("unRead" + unRead + " obj: " + o);
                                if ("close".equals(o)) {
                                    con.close();
                                }
                                con.write(ByteBuffer.wrap("pong".getBytes()));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        log.info("disconnect {}", con);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    protected void doClose() {
        AsyncService.start(() -> {
            CloseMessage closeMessage = new CloseMessage();
            closeMessage.setData(JsonService.toString("database is shutdown"));
            connectionSessionList.parallelStream().forEach(session -> session.close(closeMessage));
            log.info("server is closed");
        });
    }
}
