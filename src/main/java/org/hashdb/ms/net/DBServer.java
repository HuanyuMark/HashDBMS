package org.hashdb.ms.net;

import com.sun.jdi.connect.spi.ClosedConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CommandExecutor;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.event.StartServerEvent;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.net.client.CommandMessage;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.service.ActCommandMessage;
import org.hashdb.ms.net.service.ErrorMessage;
import org.hashdb.ms.sys.DBSystem;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JsonService;
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
import java.nio.channels.AsynchronousCloseException;
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
public class DBServer implements DisposableBean {
    private ServerSocketChannel serverChannel;

    private final DBServerConfig serverConfig;

    private final DBSystem dbSystem;

    public DBServer(DBServerConfig serverConfig, DBSystem system) {
        this.serverConfig = serverConfig;
        this.dbSystem = system;
    }

    @EventListener(StartServerEvent.class)
    public void startServer() {
        JsonService.loadConfig();
        try {//开启服务器后先广播一次确认主机，然后再进行全量数据同步
//            beginReplication(msg = new verificateMasterMessage());

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
        } catch (IOException e) {
            log.error("server start error", e);
        }
    }

    /**
     * 客户端发起连接：
     * SocketChannel clientChannel = SocketChannel.open();
     * clientChannel.bind(new InetSocketAddress(服务器ip, 服务器端口));
     */
    @SuppressWarnings("InfiniteLoopStatement")
    private void start() throws IOException {
        while (true) {
            // 接收新链接
            try {
                var connection = serverChannel.accept();          //这里的Connection是连接
                handleNewSession(connection);
            } catch (AsynchronousCloseException e) {
                break;
            }
        }
    }

//    private void beginReplication(verificateMasterMessage msg){
//        ReplicationConfig config = dbSystem.getReplicationConfig();
//        if(config.getIdentity() == ServerIdentity.MASTER){
//            //接受其他从机发来的连接
//        }else {
//            //从机向主机发起连接
//        }
//    };

    private void handleNewSession(SocketChannel con) {
        AsyncService.submit(() -> {
            // 新建新连接的会话上下文
            ConnectionSession session;           //ConnectionSession是会话
            try {
                session = new ConnectionSession(con);
            } catch (MaxConnectionException e) {
                return;
            }

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
