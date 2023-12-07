package org.hashdb.ms.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jdi.connect.spi.ClosedConnectionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.net.client.AuthenticationMessage;
import org.hashdb.ms.net.client.CloseMessage;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.service.ActAuthenticationMessage;
import org.hashdb.ms.net.service.ErrorMessage;
import org.hashdb.ms.net.service.HeartbeatMessage;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2023/11/24 16:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class ConnectionSession implements AutoCloseable {
    private static final Lazy<DBServerConfig> dbServerConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBServerConfig.class));

    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    private Database database;
    @Getter
    private final UUID id = UUID.randomUUID();

    private final SocketChannel channel;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private final BlockingDeque<Message> readDeque = new LinkedBlockingDeque<>();

    private final BlockingDeque<Message> writeDeque = new LinkedBlockingDeque<>();

    private final MessageConsumerChain chain = new MessageConsumerChain();

    private record ResultWrapper(Object result) {
    }

    private final CompletableFuture<?> messageReader;

    private final CompletableFuture<?> messageWriter;

    private final CompletableFuture<?> messageConsumer;

    private final HeartbeatMessage heartbeat;

    private long lastInteractTime;

    private boolean aliveChecked = true;

    private ScheduledFuture<?> timeoutTask;

    /**
     * 心跳机制, 确认存活
     */
    private ScheduledFuture<?> aliveChecker;

    private int failAliveCheckCount = 0;

    private volatile boolean closed = false;

    {
        // 认证
        chain.add((msg, chain) -> {
            if (!(msg instanceof AuthenticationMessage)) {
                close();
                return null;
            }

            /**
             *完成身份验证
             */

            chain.removeCurrent();
            try {
                send(new ActAuthenticationMessage());
                startCheckAlive();
            } catch (ClosedConnectionException e) {
                log.error("closedConnection throw '{}'", e.toString());
                throw ClosedConnectionWrapper.wrap(e);
            }
            return null;
        });
        // 生命周期
        chain.add((msg, chain) -> {
            lastInteractTime = System.currentTimeMillis();
            // 心跳
            actHeartbeat();
            if (msg instanceof HeartbeatMessage) {
                return null;
            }
            return chain.next();
        });
        // 处理关闭消息
        chain.add((msg, chain) -> {
            if (!(msg instanceof CloseMessage)) {
                return chain.next();
            }
            close();
            return null;
        });
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public ConnectionSession(@NotNull SocketChannel channel) throws MaxConnectionException {
        this.channel = channel;
        messageWriter = AsyncService.submit(() -> {
            while (true) {
                try {
                    Message msg = writeDeque.take();
                    send0(msg);
                } catch (ClosedConnectionException e) {
                    log.error("closedConnection throw '{}'", e.toString());
                    throw ClosedConnectionWrapper.wrap(e);
                } catch (InterruptedException e) {
                    log.error("messageSender throw '{}'", e.toString());
                    throw new WorkerInterruptedException(e);
                }
            }
        });
        messageReader = AsyncService.submit(() -> {
            while (true) {
                try {
                    Message msg = get();
                    readDeque.add(msg);
                } catch (ClosedConnectionException e) {
                    log.error("messageReader throw '{}'", e.toString());
                    throw ClosedConnectionWrapper.wrap(e);
                }
            }
        });
        messageConsumer = AsyncService.submit(() -> {
            while (true) {
                Message msg;
                try {
                    msg = readDeque.take();
                } catch (InterruptedException e) {
                    log.error("messageSender throw '{}'", e.toString());
                    throw new WorkerInterruptedException(e);
                }
                try {
                    chain.consume(msg);
                } catch (ClosedConnectionException e) {
                    log.error("closedConnection throw '{}'", e.toString());
                    throw ClosedConnectionWrapper.wrap(e);
                } catch (Exception e) {
                    log.error("message consumer chain throw '{}'", e.toString());
                    throw new DBSystemException(e);
                }
            }
        });

        if (connectionCount.getAndIncrement() > dbServerConfig.get().getMaxConnections()) {
            connectionCount.set(dbServerConfig.get().getMaxConnections());
            MaxConnectionException exception = new MaxConnectionException("out of max connection");
            try {
                send(new ErrorMessage(exception));
            } catch (ClosedConnectionException e) {
                throw ClosedConnectionWrapper.wrap(e);
            }
            close();
            throw exception;
        }
        heartbeat = HeartbeatMessage.newBeat(channel.socket());

        if (log.isInfoEnabled()) {
            log.info("new session {}", this);
        }
    }

    boolean isConnected() {
        return channel.isConnected();
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database == null) {
            close();
        } else {
            database.restrain();
        }
        this.database = database;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        readBuffer.clear();
        try {
            channel.close();
        } catch (IOException e) {
            log.error("unexpected close() throw '{}'", e.toString());
        }
        if (database != null) {
            database.release();
        }
        aliveChecker.cancel(true);
        messageWriter.cancel(true);
        messageReader.cancel(true);
        messageConsumer.cancel(true);
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
        }
        closed = true;
        if (log.isInfoEnabled()) {
            log.info("close session {}", this);
        }
    }

    protected void startCheckAlive() {
        long heartbeatInterval = dbServerConfig.get().getHeartbeatInterval();
        int timeoutRetry = dbServerConfig.get().getTimeoutRetry();
        aliveChecker = AsyncService.setInterval(() -> {
            if (System.currentTimeMillis() - lastInteractTime <= heartbeatInterval) {
                actHeartbeat();
                return;
            }
            if (aliveChecked) {
                aliveChecked = false;
                try {
                    send(heartbeat);
                    heartbeat.next();
                } catch (ClosedConnectionException e) {
                    log.warn("unexpected send heartbeat closed throw '{}'", e.toString());
                    throw ClosedConnectionWrapper.wrap(e);
                }
                return;
            }
            if (failAliveCheckCount++ < timeoutRetry) {
                return;
            }
            if (log.isInfoEnabled()) {
                log.info("inactive session");
            }
            close();
        }, heartbeatInterval);
    }

    protected Message get() throws ClosedConnectionException, IllegalMessageException {
        if (closed || !channel.isConnected()) {
            throw new ClosedConnectionException("Connection closed");
        }
        try {
            int readCount;
            int expectReadCount = readBuffer.position();
            while ((readCount = channel.read(readBuffer)) <= expectReadCount) {
                if (readCount == -1) {
                    throw new IllegalStateException("no data can read in read buffer");
                }
                expectReadCount -= readCount;
            }
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
        var json = new String(Arrays.copyOf(readBuffer.array(), readBuffer.position()));
        try {
            Message result = JsonService.parse(json, Message.class);
            readBuffer.clear();
            if (result == null) {
                throw new IllegalMessageException("unexpected message type 'null'");
            }
            return result;
        } catch (JsonProcessingException e) {
            readBuffer.clear();
            throw new IllegalMessageException("unexpected message '" + json + "'");
        }
    }

    public void send(Message toSend) throws ClosedConnectionException {
        writeDeque.add(toSend);
    }

    public void addMessageConsumer(MessageConsumer consumer) {
        chain.add(consumer);
    }

    protected void send0(Message toSend) throws ClosedConnectionException {
        if (closed || !channel.isConnected()) {
            throw new ClosedConnectionException("Connection closed");
        }
        String data = JsonService.stringfy(toSend);
        try {
            channel.write(ByteBuffer.wrap(data.getBytes()));
            lastInteractTime = System.currentTimeMillis();
        } catch (IOException e) {
            throw new DBSystemException(e);
        }
    }

    @Override
    public String toString() {
        return "ConnectionSession{" +
                ", id=" + id +
                ", channel=" + channel +
                '}';
    }

    protected void actHeartbeat() {
        aliveChecked = true;
        failAliveCheckCount = 0;
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }
}
