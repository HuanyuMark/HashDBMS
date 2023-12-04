package org.hashdb.ms.net;

import com.sun.jdi.connect.spi.ClosedConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CommandExecutor;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.net.client.CommandMessage;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.service.ActCommandMessage;
import org.hashdb.ms.net.service.ErrorMessage;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.Runners;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2023/12/1 1:26
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DBServer implements DisposableBean {
    private ServerSocketChannel serverChannel;

    private final DBServerConfig serverConfig;

    private CompletableFuture<?> connectionHandler;

    private int connectionCount = 0;

    @EventListener(StartServerEvent.class)
    public void ready() throws IOException {
        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(true);
            serverChannel.bind(new InetSocketAddress(serverConfig.getPort()));
        } catch (BindException e) {
            log.error("port {} is in use", serverConfig.getPort());
            throw e;
        }
        log.info("server is running at port: {}", serverConfig.getPort());
        start();
    }

    private void start() throws IOException {
        while (true) {
            // 接收新链接
            var connection = serverChannel.accept();
            handleNewSession(connection);
        }
    }

    private void handleNewSession(SocketChannel con) {
        AsyncService.submit(() -> {
            // 新建新连接的会话上下文
            var session = new ConnectionSession(con);
            int newConnectionCount = connectionCount + 1;
            if (newConnectionCount > serverConfig.getMaxConnections()) {
                try {
                    session.send(new ErrorMessage(new MaxConnectionException("out of max connection")));
                } catch (ClosedConnectionException e) {
                    throw ClosedConnectionWrapper.wrap(e);
                }
                return;
            }
            connectionCount = newConnectionCount;

            // 根据会话创建会话特化的编译器
            var commandExecutor = CommandExecutor.create(session);
            // 添加命令消息的处理器
            session.addMessageConsumer((msg, chain) -> {
                if (!(msg instanceof CommandMessage commandMessage)) {
                    return chain.next();
                }

                Message toSend;
                try {
                    // 取得命令运行结果
                    log.info("run command |{}", commandMessage.getCommand());
                    var result = commandExecutor.run(commandMessage.getCommand());
                    log.info("send result |{}", result);
                    toSend = new ActCommandMessage(commandMessage, result);
                } catch (DBClientException e) {
                    // 如果有异常,就发送
                    toSend = new ErrorMessage(e);
                    log.info("send error  |{}", toSend);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("command runner throw '{}'", e.toString());
                    toSend = new ErrorMessage(new CommandExecuteException(e));
                }
                try {
                    session.send(toSend);
                } catch (ClosedConnectionException ex) {
                    log.warn("unexpected send error msg closed throw '{}'", ex.toString());
                    throw ClosedConnectionWrapper.wrap(ex);
                } catch (Exception e) {
                    log.error("unexpected error", e);
                    throw new DBSystemException(e);
                }
                return null;
            });
        });
    }

    @Override
    public void destroy() throws Exception {
//        connectionHandler.cancel(true);
        serverChannel.close();
    }

    @Contract(" -> new")
    private @NotNull CompletableFuture<?> test() {
        return AsyncService.submit(() -> Runners.everlasting(() -> {
            try {
                var con = serverChannel.accept();

                ByteBuffer buffer = ByteBuffer.allocate(1024);
                AsyncService.submit(() -> {
                    try (
                            ConnectionSession session = new ConnectionSession(con);
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


    private void demo() {
//        connectionHandler = start();
//        connectionHandler.join();
//        log.info("Server is ready");
//        Scanner scanner = new Scanner(System.in);
//        ConnectionSession session = new ConnectionSession();
//        Compiler compiler = Compiler.create(session);
//        while (true) {
//            try {
//                String command = scanner.nextLine();
//                if(command.equals("exit")){
//                    System.exit(0);
//                }
//                String result = compiler.run(command);
//                System.out.println(result);
//            } catch (RuntimeException e) {
//                e.printStackTrace();
//            }
//        }
    }
}
