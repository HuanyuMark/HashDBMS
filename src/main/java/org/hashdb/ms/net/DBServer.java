package org.hashdb.ms.net;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.event.CloseServerEvent;
import org.hashdb.ms.event.StartServerEvent;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.util.JsonService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * Date: 2023/12/1 1:26
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class DBServer implements DisposableBean, AutoCloseable {

    protected final DBServerConfig serverConfig;

    protected final DBSystem dbSystem;

    public DBServer(DBServerConfig serverConfig, DBSystem system) {
        this.serverConfig = serverConfig;
        this.dbSystem = system;
    }

    @EventListener(StartServerEvent.class)
    public void startServer() throws IOException {
        JsonService.loadConfig();
        try {//开启服务器后先广播一次确认主机，然后再进行全量数据同步
//            beginReplication(msg = new verificateMasterMessage());
            var serverChanel = ServerSocketChannel.open();
            try {
                serverChannelOptimizer(serverChanel);
                serverChanel.bind(new InetSocketAddress(serverConfig.getPort()));
            } catch (BindException e) {
                log.error("port {} is in use", serverConfig.getPort());
                System.exit(1);
                throw e;
            }
            log.info("server is running at port: {}", serverConfig.getPort());
            doStart(serverChanel);
        } catch (IOException e) {
            log.error("server start error", e);
        }
    }

    protected abstract void serverChannelOptimizer(ServerSocketChannel serverSocketChannel) throws IOException;

    /**
     * 客户端发起连接：
     * SocketChannel clientChannel = SocketChannel.open();
     * clientChannel.bind(new InetSocketAddress(服务器ip, 服务器端口));
     */
    protected abstract void doStart(ServerSocketChannel serverChannel) throws IOException;

    @Override
    @EventListener(CloseServerEvent.class)
    public void close() throws Exception {
        doClose();
    }

    abstract protected void doClose() throws Exception;


    //    private void beginReplication(verificateMasterMessage msg){
//        ReplicationConfig config = dbSystem.getReplicationConfig();
//        if(config.getIdentity() == ServerIdentity.MASTER){
//            //接受其他从机发来的连接
//        }else {
//            //从机向主机发起连接
//        }
//    };


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
