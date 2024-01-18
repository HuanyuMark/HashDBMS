package org.hashdb.ms.net.bio;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CommandExecutor;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.WorkerInterruptedException;
import org.hashdb.ms.net.AbstractConnectionSession;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.net.client.ActHeartbeatMessage;
import org.hashdb.ms.net.client.AuthenticationMessage;
import org.hashdb.ms.net.client.CloseMessage;
import org.hashdb.ms.net.client.CommandMessage;
import org.hashdb.ms.net.exception.IllegalAccessException;
import org.hashdb.ms.net.exception.*;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.service.ActAuthenticationMessage;
import org.hashdb.ms.net.service.ActCommandMessage;
import org.hashdb.ms.net.service.ErrorMessage;
import org.hashdb.ms.net.service.HeartbeatMessage;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.CacheMap;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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
public class BIOConnectionSession extends AbstractConnectionSession implements ConnectionSession {

    private String user;

    private static final AtomicInteger connectionCount = new AtomicInteger(0);

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

    private long lastGetTime;

    private boolean aliveChecked = true;

    private ScheduledFuture<?> timeoutTask;

    /**
     * 心跳机制, 确认存活
     */
    private ScheduledFuture<?> aliveChecker;

    private int failAliveCheckCount = 0;

    /**
     * 不接收新请求, 用于关闭连接
     */
    // TODO: 2024/1/15 实现关闭逻辑
    private volatile boolean closing = false;

    {
        localCommandCache = new CacheMap<>(ConnectionSession.dbServerConfig.get().getCommandCache().getAliveDuration(), ConnectionSession.dbServerConfig.get().getCommandCache().getCacheSize());
        var requestWithNoAuth = new int[]{0};
        Function<Integer, ScheduledFuture<?>> getCloseNoAuthSessionTask = timeout -> AsyncService.setTimeout(() -> {
            if (user != null) {
                return;
            }
            CloseMessage closeMessage = new CloseMessage();
            closeMessage.setData(JsonService.toString("authentication timeout"));
            close(closeMessage);
        }, timeout);
        var closeNoAuthSessionTask = new ScheduledFuture[]{
                getCloseNoAuthSessionTask.apply(30 * 60 * 1000)
        };
        // 认证
        chain.add((msg, chain) -> {
            // if auth is passed
            if (msg instanceof CloseMessage) {
                return chain.next();
            }
            if (!(msg instanceof AuthenticationMessage authMsg)) {
                if (user != null) {
                    return chain.next();
                }
                if (requestWithNoAuth[0]++ > 3) {
                    var closeMessage = new CloseMessage();
                    closeMessage.setData(JsonService.toString("require authenticate"));
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
                } catch (ClosedChannelException e) {
                    log.error("send no   auth  throw", e);
                    throw ClosedChannelWrapper.wrap(e);
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
            Database userDb = ConnectionSession.dbSystem.get().getDatabase("user");
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
                act.setData(JsonService.toString("SUCC"));
                send(act);
                startCheckAlive();
            } catch (ClosedChannelException e) {
                log.error("send auth pass  throw", e);
                throw ClosedChannelWrapper.wrap(e);
            }
            return null;
        });
        // 生命周期
        chain.add((msg, chain) -> {
            lastGetTime = System.currentTimeMillis();
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

        // 根据会话创建会话特化的编译器
        var commandExecutor = CommandExecutor.create(this);
        // 添加命令消息的处理器
        chain.add((msg, chain) -> {
            if (!(msg instanceof CommandMessage commandMessage)) {
                return chain.next();
            }

            Message toSend;
            try {
                // 取得命令运行结果
                log.info("run command |'{}'", commandMessage.getCommand());
                var result = commandExecutor.run(commandMessage.getCommand());
                toSend = new ActCommandMessage(commandMessage, result);
            } catch (DBClientException e) {
                // 如果有异常,就发送
                toSend = new ErrorMessage(e);
            } catch (Exception e) {
                log.error("command runner throw ", e);
                toSend = new ErrorMessage(new CommandExecuteException(e));
            }
            try {
                log.info("send result |{}", toSend);
                send(toSend);
            } catch (ClosedChannelException ex) {
                log.warn("unexpected send error msg closed throw '{}'", ex.toString());
                throw ClosedChannelWrapper.wrap(ex);
            } catch (Exception e) {
                log.error("unexpected error", e);
                throw new DBSystemException(e);
            }
            return null;
        });
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public BIOConnectionSession(@NotNull SocketChannel channel) throws MaxConnectionException {
        this.channel = channel;
        messageWriter = AsyncService.start(() -> {
            while (true) {
                try {
                    Message msg = writeQueue.take();
                    send0(msg);
                } catch (ClosedChannelException e) {
                    if (log.isTraceEnabled()) {
                        log.error("messageWriter throw ", e);
                    }
                    throw ClosedChannelWrapper.wrap(e);
                } catch (InterruptedException e) {
                    if (log.isTraceEnabled()) {
                        log.error("messageWriter throw ", e);
                    }
                    throw new WorkerInterruptedException(e);
                } catch (Exception e) {
                    log.error("messageWriter throw unexpected", e);
                    throw new DBSystemException(e);
                }
            }
        });
        messageReader = AsyncService.start(() -> {
            while (true) {
                try {
                    if (closing) {
                        return;
                    }
                    Message msg = get();
//                    System.out.println("messageReader read msg: " + msg.getType());
                    readQueue.add(msg);
                } catch (ClosedChannelException e) {
                    if (log.isTraceEnabled()) {
                        log.error("messageReader throw ", e);
                    }
                    throw ClosedChannelWrapper.wrap(e);
                } catch (DBSystemException e) {
                    if (e.getCause() instanceof ClosedChannelException) {
                        if (log.isTraceEnabled()) {
                            log.error("messageReader throw ", e);
                        }
                        return;
                    }
                    log.error("messageReader throw unexpected", e);
                    throw e;
                } catch (Exception e) {
                    log.error("messageReader throw unexpected", e);
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
                } catch (ClosedChannelException e) {
                    log.error("closedConnection throw", e);
                    throw ClosedChannelWrapper.wrap(e);
                } catch (Exception e) {
                    log.error("message consumer chain throw", e);
                    throw new DBSystemException(e);
                }
            }
        });

        if (connectionCount.getAndIncrement() > ConnectionSession.dbServerConfig.get().getMaxConnections()) {
            connectionCount.set(ConnectionSession.dbServerConfig.get().getMaxConnections());
            MaxConnectionException exception = new MaxConnectionException("out of max connection");
            try {
                send(new ErrorMessage(exception));
            } catch (ClosedChannelException e) {
                throw ClosedChannelWrapper.wrap(e);
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
    protected void doClose() {
        doClose(null);
    }

    @Override
    protected void doClose(CloseMessage message) {
        super.doClose();
        if (message != null) {
            writeQueue.clear();
            try {
                send0(message);
            } catch (ClosedChannelException ignore) {
                // can not connect to client
            }
        }
        try {
            channel.close();
        } catch (IOException e) {
            log.error("unexpected close() throw", e);
        }
        if (aliveChecker != null) {
            aliveChecker.cancel(true);
        }
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

    protected void startCheckAlive() {
        if (aliveChecker != null) {
            return;
        }
        long heartbeatInterval = ConnectionSession.dbServerConfig.get().getHeartbeatInterval();
        int timeoutRetry = ConnectionSession.dbServerConfig.get().getTimeoutRetry();
        aliveChecker = AsyncService.setInterval(() -> {
            if (System.currentTimeMillis() - lastGetTime <= heartbeatInterval) {
                actHeartbeat();
                return;
            }
            if (aliveChecked) {
                aliveChecked = false;
                try {
                    send(heartbeat);
                    heartbeat.next();
                } catch (ClosedChannelException e) {
                    log.warn("unexpected send heartbeat closed throw ", e);
                    throw ClosedChannelWrapper.wrap(e);
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
            closeMessage.setData(JsonService.toString("heartbeat timeout"));
            close(closeMessage);
        }, heartbeatInterval);
    }

    protected Message get() throws ClosedChannelException, IllegalMessageException {
        if (closed || !channel.isConnected()) {
            close();
            throw new ClosedChannelException();
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
            close();
            throw new DBSystemException(e);
        }
        var json = new String(Arrays.copyOf(readBuffer.array(), readBuffer.position()));
        if (json.isEmpty() || !channel.isConnected()) {
            throw new ClosedChannelException();
        }
        try {
            Message result = JsonService.parse(json, Message.class);
            readBuffer.clear();
            if (result == null) {
                throw new IllegalMessageException("unexpected message type 'null'");
            }
            log.info("get msg: {}", result);
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

    public void send(Message toSend) throws ClosedChannelException {
        writeQueue.add(toSend);
    }

    public void addMessageConsumer(MessageConsumer consumer) {
        chain.add(consumer);
    }

    protected void send0(Message toSend) throws ClosedChannelException {
        if (closed || !channel.isConnected()) {
            close();
            throw new ClosedChannelException();
        }
        String data = JsonService.toString(toSend);
        log.info("send msg: {}", data);
        try {
            channel.write(ByteBuffer.wrap(data.getBytes()));
        } catch (IOException e) {
            close();
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
        } catch (ClosedChannelException e) {
            log.error("send auth error throw", e);
            throw ClosedChannelWrapper.wrap(e);
        }
    }

}
