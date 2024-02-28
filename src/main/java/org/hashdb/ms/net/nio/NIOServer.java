package org.hashdb.ms.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.DBServer;
import org.hashdb.ms.net.bio.client.CloseMessage;
import org.hashdb.ms.net.exception.ClosedChannelWrapper;
import org.hashdb.ms.support.StaticScanIgnore;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JsonService;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Date: 2024/1/15 14:55
 * 使用了NIO实现
 *
 * @author Huanyu Mark
 */
@Slf4j
@Deprecated
@StaticScanIgnore
public class NIOServer extends DBServer {

    private volatile boolean epoll = true;

    private volatile boolean closing = false;
    private final Selector selector = Selector.open();

    private ServerSocketChannel serverChannel;

    public NIOServer(DBServerConfig serverConfig, DBSystem system) throws IOException {
        super(serverConfig, system);
    }

    @Override
    protected void serverChannelOptimizer(ServerSocketChannel serverSocketChannel) throws IOException {
        this.serverChannel.configureBlocking(false);  // 设置为非阻塞模式
    }

    @Override
    protected void doStart(ServerSocketChannel serverChannel) throws IOException {
        // 创建一个ServerSocketChannel并绑定端口
        this.serverChannel = serverChannel;
        // 注册ServerSocketChannel到Selector上监听接受新连接事件
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        while (epoll) {
            selector.select();
            // 轮询准备好的事件
            var selectionKeyItr = selector.selectedKeys().iterator();
            while (selectionKeyItr.hasNext()) {
                var key = selectionKeyItr.next();
                if (!key.isValid()) {
                    log.info("remove invalid key {}", toString(key));
                    selectionKeyItr.remove();
                    continue;
                }
                if (key.isAcceptable()) {
                    var clientChannel = this.serverChannel.accept();
                    var session = new NIOConnectionSession(clientChannel).onClosed(key::cancel);
//                    sessionMap.put(clientChannel, session);
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, session);
                    selectionKeyItr.remove();
                    continue;
                }
                if (key.isReadable()) {
                    responseClient(key, true);
                    selectionKeyItr.remove();
                } else if (key.isWritable()) {
                    responseClient(key, false);
                    selectionKeyItr.remove();
                } else {
                    log.warn("a kay has not been handled {}", toString(key));
                }
            }
        }
    }

    private void responseClient(SelectionKey key, boolean readable) throws IOException {
        // 如果有至少一个事件准备就绪
        // 处理读事件
        SocketChannel clientChannel = (SocketChannel) key.channel();
        var session = ((NIOConnectionSession) key.attachment());
        try {
            if (readable) {
                session.responseRead();
                return;
            }
            session.responseWrite(key);
        } catch (ClosedChannelWrapper e) {
            // 意外关闭
            session.close();
        }
    }

    @Override
    protected void doClose() throws Exception {
        if (!epoll || closing) {
            return;
        }
        epoll = false;
        closing = true;
        selector.wakeup();
        AsyncService.start(() -> {
            CloseMessage closeMessage = new CloseMessage();
            closeMessage.setData(JsonService.toString("database is shutdown"));
            selector.keys().parallelStream().forEach(key -> {
                if (key.attachment() instanceof NIOConnectionSession session) {
                    session.close();
                }
            });
            try {
                selector.close();
            } catch (IOException e) {
                log.error("close server error", e);
            }
            try {
                serverChannel.close();
            } catch (IOException e) {
                log.error("close server error", e);
            }
            log.info("server is closed");
            closing = false;
        });
    }

    @Override
    public void destroy() throws Exception {

    }

    public static String toString(SelectionKey key) {
        var acceptable = key.isAcceptable() ? "Acceptable" : "";
        var readable = key.isReadable() ? "Readable" : "";
        var writable = key.isWritable() ? "Writable" : "";
        var connectable = key.isConnectable() ? "Connectable" : "";
        var valid = key.isValid() ? "Valid" : "";

        return key + "(" + acceptable + readable + writable + connectable + valid + ")";
    }
}
