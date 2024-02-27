package org.hashdb.ms.net.nio;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.LocalCommandExecutor;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.AbstractConnectionSession;
import org.hashdb.ms.net.bio.client.CloseMessage;
import org.hashdb.ms.net.bio.client.CommandMessage;
import org.hashdb.ms.net.bio.msg.Message;
import org.hashdb.ms.net.bio.service.ErrorMessage;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Date: 2024/1/15 23:24
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Deprecated
public class NIOConnectionSession extends AbstractConnectionSession {
    private final SocketChannel channel;

    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

    private ByteBuffer writeBuffer;
    //    private final List<ByteBuffer> responseBuffers = new LinkedList<>();
    private final List<Message> responseMessage = new LinkedList<>();

    private final List<Object> handleResult = new LinkedList<>();
    private String username;

    private static int maxId;
    private final int id = ++maxId;

    private final LocalCommandExecutor commandExecutor = LocalCommandExecutor.create(this);

    private static int maxConnections = 0;

    //    private final CompletableFuture<?> messageDeserializer = AsyncService.start(() -> {
//        try {
//            Message message = responseMessage.take();
//            String json = JsonService.stringfy(message);
//            responseBuffers.add(ByteBuffer.wrap(json.getBytes()));
//        } catch (InterruptedException e) {
//            if (log.isTraceEnabled()) {
//                log.trace("messageDeserializer interrupted", e);
//            }
//        }
//    });


    private Runnable closedCb;

    NIOConnectionSession(SocketChannel channel) {
        this.channel = channel;
        ++maxConnections;
    }

    void responseRead() throws ClosedChannelException {
        try {
            for (int readCount = channel.read(buffer); true; readCount = channel.read(buffer)) {
                if (readCount < 0) {
                    close();
                    return;
                }
                // 存在边界问题, 分多次读事件读取
                if (!buffer.hasRemaining()) {
                    ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() << 1);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                    continue;
                }
                buffer.flip();
                Message message;
                try {
//                    StandardCharsets.UTF_8.de
                    // 边界问题
                    message = JsonService.parse(buffer.array(), buffer.position(), readCount, Message.class);
                    buffer.clear();
                    if (message == null) {
                        throw new IOException("message is null");
                    }
                } catch (IOException e) {
                    log.error("unexpected message ", e);
                    break;
                }

                Message response = dispatchMessage(message);
                if (response != null) {
                    responseMessage.add(response);
                }
            }
        } catch (ClosedChannelException e) {
            throw e;
        } catch (IOException e) {
            log.warn("{} responseRead", this, e);
        }
    }

    // TODO: 2024/1/16 完成有关于消息处理的逻辑
    private Message dispatchMessage(Message message) {
        // TODO: 2024/1/16 身份验证
        // TODO: 2024/1/16 处理心跳
        // TODO: 2024/1/16 处理关闭
        if (message instanceof CommandMessage msg) {
            String command = msg.getCommand();
            try {
                // TODO: 2024/1/16 获取结果返回的byte数组,不要获取String
                String result = commandExecutor.run(command);
            } catch (ExecutionException e) {
                log.error("command {} error", command, e.getCause());
            }
        }
        return null;
    }

    void responseWrite(SelectionKey key) {
        if (writeBuffer != null) {
            int writeCount;
            try {
                writeCount = channel.write(writeBuffer);
            } catch (IOException e) {
                log.error("writer remaining buffer error", e);
                return;
            }
            if (writeCount < 0) {
                close();
                return;
            }
            // 还没读完, 直接返回,轮到下次select的时候写就绪再写
            if (writeBuffer.hasRemaining()) {
                return;
            }
            writeBuffer = null;
        }
        if (responseMessage.isEmpty()) {
            return;
        }
        try {
            long start = System.currentTimeMillis();
            while (responseMessage.size() > 0) {
                var json = JsonService.toBytes(responseMessage.removeFirst());
                writeBuffer = ByteBuffer.wrap(json);
                // 0 网络写缓冲区满
                // -1 通断断开
                int writeCount = channel.write(writeBuffer);
                if (writeCount < 0) {
                    close();
                    return;
                }
                // 未全部写入, 等下次写就绪再写
                if (writeBuffer.hasRemaining()) {
                    return;
                }
            }
            // 已经全部写入, 清除缓存
            writeBuffer = null;
            log.info("{} responseWrite cost {}ms", this, System.currentTimeMillis() - start);
        } catch (ClosedChannelException e) {
            log.warn("{} responseWrite close connection {}", this, e.getCause());
            close();
        } catch (DBSystemException e) {
            log.warn("{} responseWrite", this, e.getCause());
            ErrorMessage errorMessage = new ErrorMessage(new DBClientException("server error"));
            responseMessage.addFirst(errorMessage);
        } catch (IOException e) {
            log.warn("{} responseRead", this, e);
        }
    }

    @Override
    protected void doClose() {
        doClose(null);
    }

    @Override
    protected void doClose(CloseMessage message) {
        --maxConnections;
        super.doClose();
        buffer = null;
        writeBuffer = null;
        try {
            channel.close();
        } catch (IOException e) {
            log.warn("do close", e);
        }
        if (closedCb != null) {
            closedCb.run();
        }
    }

    NIOConnectionSession onClosed(Runnable cb) {
        closedCb = cb;
        return this;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    @SneakyThrows
    public String toString() {
        return "session[" + id + "](IP=" +
                (channel.isOpen() ? channel.getRemoteAddress() : null)
                + "," + (closed ? "closed" : "open")
                + ")";
    }
}
