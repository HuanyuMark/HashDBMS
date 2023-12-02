package org.hashdb.ms.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CommandExecutor;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.exception.ClosedConnectionException;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.MaxConnectionException;
import org.hashdb.ms.net.client.CloseMessage;
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
            SocketChannel connection = serverChannel.accept();
            handleNewSession(connection);
        }
    }

    private void handleNewSession(SocketChannel con) {
        AsyncService.submit(() -> {
            // 新建新连接的会话上下文
            try (ConnectionSession session = new ConnectionSession(con)) {
                int newConnectionCount = connectionCount + 1;
                if(newConnectionCount > serverConfig.getMaxConnections()) {
                    session.send(new ErrorMessage(new MaxConnectionException("out of max connection")));
                    return;
                }
                connectionCount = newConnectionCount;

                // 根据会话创建会话特化的编译器
                CommandExecutor commandExecutor = CommandExecutor.create(session);
                while (session.isConnected()) {
                    // 接收命令消息
                    Message message = session.get(Message.class);
                    if(message instanceof CloseMessage) {
                        break;
                    }

                    if(!(message instanceof CommandMessage commandMessage)) {
                        throw new RuntimeException("unexpected message :"+message);
                    }

                    String result;
                    try {
                        // 取得命令运行结果
                        result = commandExecutor.run(commandMessage.getCommand());
                    } catch (DBClientException e) {
                        // 如果有异常,就发送
                        ErrorMessage errorMessage = new ErrorMessage(e);
                        session.send(errorMessage);
                        continue;
                    } catch (Exception e) {
                        log.error("unexpected error", e);
                        break;
                    }
                    ActCommandMessage act = new ActCommandMessage(commandMessage, result);
                    session.send(act);
                }
            } catch (ClosedConnectionException ignore) {
            }
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


    private void demo(){
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
