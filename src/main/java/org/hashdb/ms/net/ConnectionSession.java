package org.hashdb.ms.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jdi.connect.spi.ClosedConnectionException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.IllegalAccessException;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.net.client.ActHeartbeatMessage;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Date: 2023/11/24 16:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class ConnectionSession implements AutoCloseable, ReadonlyConnectionSession {
    private static final Lazy<DBServerConfig> dbServerConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBServerConfig.class));

    private static final Lazy<DBSystem> dbSystem = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBSystem.class));

    private String user;

    private static final AtomicInteger connectionCount = new AtomicInteger(0);
    @Nullable
    private Database database;
    @Getter
    private final UUID id = UUID.randomUUID();

    private final SocketChannel channel;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private final BlockingQueue<Message> readQueue = new LinkedBlockingQueue<>();

    private final BlockingQueue<Message> writeQueue = new LinkedBlockingQueue<>();

    private final MessageConsumerChain chain = new MessageConsumerChain();

    private record ResultWrapper(Object result) {
    }

    private final CompletableFuture<?> messageReader;

    private final CompletableFuture<?> messageWriter;

    private final CompletableFuture<?> messageConsumerDispatcher;

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
        var requestWithNoAuth = new int[]{0};
        Function<Integer, ScheduledFuture<?>> getCloseNoAuthSessionTask = timeout -> AsyncService.setTimeout(() -> {
            if (user != null) {
                return;
            }
            CloseMessage closeMessage = new CloseMessage();
            closeMessage.setData(JsonService.stringfy("authentication timeout"));
            close(closeMessage);
        }, timeout);
        var closeNoAuthSessionTask = new ScheduledFuture[]{
                getCloseNoAuthSessionTask.apply(30 * 60 * 1000)
        };
        // 认证
        chain.add((msg, chain) -> {
            // if auth is passed
            if (user != null) {
                return chain.next();
            }
            if (msg instanceof CloseMessage) {
                return chain.next();
            }
            if (!(msg instanceof AuthenticationMessage authMsg)) {
                if (requestWithNoAuth[0]++ > 3) {
                    var closeMessage = new CloseMessage();
                    closeMessage.setData(JsonService.stringfy("require authenticate"));
                    if (closeNoAuthSessionTask[0] != null) {
                        closeNoAuthSessionTask[0].cancel(true);
                    }
                    close(closeMessage);
                    return null;
                }
//                var act = new ActAuthenticationMessage(false);
//                act.setData(JsonService.stringfy("require authenticate"));
                if (closeNoAuthSessionTask[0] != null) {
                    closeNoAuthSessionTask[0].cancel(true);
                    closeNoAuthSessionTask[0] = getCloseNoAuthSessionTask.apply(3000);
                }
                var errorMessage = new ErrorMessage(new IllegalAccessException("require authenticate"));
                try {
                    send(errorMessage);
                } catch (ClosedConnectionException e) {
                    log.error("send no   auth  throw", e);
                    throw ClosedConnectionWrapper.wrap(e);
                }
                return null;
            }

            // start auth
            // if the password in passwordAuth is null
            // or these user is not exist
            // or password is not equal
            if (authMsg.getPasswordAuth().password() == null) {
                return sendAuthFailedMsg();
            }
            Database userDb = dbSystem.get().getDatabase("user");
            @SuppressWarnings("unchecked")
            Map<String, String> user = (Map<String, String>) HValue.unwrapData(userDb.submitOpsTaskSync(OpsTask.of(() -> userDb.get(authMsg.getPasswordAuth().username()))));
            if (user == null || !user.get("password").equals(authMsg.getPasswordAuth().password())) {
                return sendAuthFailedMsg();
            }

            // auth pass
            try {
                this.user = authMsg.getPasswordAuth().username();
                if (closeNoAuthSessionTask[0] != null) {
                    closeNoAuthSessionTask[0].cancel(true);
                }
                var act = new ActAuthenticationMessage();
                act.setUser(authMsg.getPasswordAuth().username());
                act.setData(JsonService.stringfy("SUCC"));
                send(act);
                startCheckAlive();
            } catch (ClosedConnectionException e) {
                log.error("send auth pass  throw", e);
                throw ClosedConnectionWrapper.wrap(e);
            }
            return null;
        });
        // 生命周期
        chain.add((msg, chain) -> {
            lastInteractTime = System.currentTimeMillis();
            // 心跳
            actHeartbeat();
            if (msg instanceof ActHeartbeatMessage) {
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
        messageWriter = AsyncService.start(() -> {
            while (true) {
                try {
                    Message msg = writeQueue.take();
                    send0(msg);
                } catch (ClosedConnectionException e) {
                    if (log.isTraceEnabled()) {
                        log.error("messageWriter throw ", e);
                    }
                    throw ClosedConnectionWrapper.wrap(e);
                } catch (InterruptedException e) {
                    if (log.isTraceEnabled()) {
                        log.error("messageWriter throw ", e);
                    }
                    throw new WorkerInterruptedException(e);
                } catch (Exception e) {
                    log.error("messageWriter throw ", e);
                    throw new DBSystemException(e);
                }
            }
        });
        messageReader = AsyncService.start(() -> {
            while (true) {
                try {
                    Message msg = get();
//                    System.out.println("messageReader read msg: " + msg.getType());
                    readQueue.add(msg);
                } catch (ClosedConnectionException e) {
                    log.error("messageReader throw '{}'", e.toString());
                    throw ClosedConnectionWrapper.wrap(e);
                } catch (Exception e) {
                    log.error("messageReader throw ", e);
                    throw new DBSystemException(e);
                }
            }
        });
        messageConsumerDispatcher = AsyncService.start(() -> {
            while (true) {
                Message msg;
                try {
                    msg = readQueue.take();
                } catch (InterruptedException e) {
                    log.error("messageConsumer throw", e);
                    throw new WorkerInterruptedException(e);
                } catch (Exception e) {
                    log.error("messageConsumer throw ", e);
                    throw new DBSystemException(e);
                }
                try {
//                    System.out.println("messageConsumer consume msg: " + msg.getType());
                    chain.consume(msg);
                } catch (ClosedConnectionException e) {
                    log.error("closedConnection throw", e);
                    throw ClosedConnectionWrapper.wrap(e);
                } catch (Exception e) {
                    log.error("message consumer chain throw", e);
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
            // TODO: 2024/1/10 提示数据库连接过多
            close();
            throw exception;
        }
        heartbeat = HeartbeatMessage.newBeat(channel.socket());

        if (log.isInfoEnabled()) {
            log.info("new session: {}", this);
        }
    }

    boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public @Nullable Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database == null) {
            close();
        } else {
            database.use();
        }
        this.database = database;
    }

    public synchronized void close(CloseMessage message) {
        if (closed) {
            return;
        }
        if (message != null) {
            writeQueue.clear();
            try {
                send0(message);
            } catch (ClosedConnectionException ignore) {
                // can not connect to client
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
            log.error("unexpected close() throw", e);
        }
        if (database != null) {
            database.release();
        }
        aliveChecker.cancel(true);
        messageWriter.cancel(true);
        messageReader.cancel(true);
        messageConsumerDispatcher.cancel(true);
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
        }
        closed = true;
        if (log.isInfoEnabled()) {
            log.info("close session {}", this);
        }
    }

    @Override
    public synchronized void close() {
        close(null);
    }

    protected void startCheckAlive() {
        if (aliveChecker != null) {
            return;
        }
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
                    log.warn("unexpected send heartbeat closed throw ", e);
                    throw ClosedConnectionWrapper.wrap(e);
                }
                return;
            }
            if (failAliveCheckCount++ < timeoutRetry) {
                log.warn("failAliveCheckCount: {}", failAliveCheckCount);
                return;
            }
            if (log.isInfoEnabled()) {
                log.info("inactive session {}", this);
            }
            var closeMessage = new CloseMessage();
            closeMessage.setData(JsonService.stringfy("heartbeat timeout"));
            close(closeMessage);
        }, heartbeatInterval);
    }

    protected Message get() throws ClosedConnectionException, IllegalMessageException {
        if (closed || !channel.isConnected()) {
            throw new ClosedConnectionException("Connection closed");
        }
        try {
            channel.read(readBuffer);
//            int readCount;
//            int expectReadCount = readBuffer.position();
//            while ((readCount = channel.read(readBuffer)) <= expectReadCount) {
//                if (readCount == -1) {
//                    throw new IllegalStateException("no data can read in read buffer");
//                }
//                expectReadCount -= readCount;
//            }
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
            log.error("can not parse msg", e);
            throw new IllegalMessageException("unexpected message '" + json + "'");
        } catch (Exception e) {
            log.error("unexpected message '{}' throw '{}'", json, e.toString());
            throw e;
        }
    }

    public void send(Message toSend) throws ClosedConnectionException {
        writeQueue.add(toSend);
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

    protected Object sendAuthFailedMsg() {
        var errorMessage = new ErrorMessage(new AuthenticationFailedException("Incorrect username or password"));
        try {
            send(errorMessage);
            return null;
        } catch (ClosedConnectionException e) {
            log.error("send auth error throw", e);
            throw ClosedConnectionWrapper.wrap(e);
        }
    }
}
