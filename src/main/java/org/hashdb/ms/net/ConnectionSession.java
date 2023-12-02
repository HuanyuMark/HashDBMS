package org.hashdb.ms.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.ClosedConnectionException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.IllegalMessageException;
import org.hashdb.ms.net.client.ActHeartbeatMessage;
import org.hashdb.ms.net.client.AuthenticationMessage;
import org.hashdb.ms.net.msg.Message;
import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.service.ActAuthenticationMessage;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;

/**
 * Date: 2023/11/24 16:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class ConnectionSession implements AutoCloseable {
    private static final Lazy<DBServerConfig> dbServerConfig = Lazy.of(() -> HashDBMSApp.ctx().getBean(DBServerConfig.class));
    private Database database;

    @Getter
    private final UUID id = UUID.randomUUID();

    private final SocketChannel channel;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    private HeartbeatMessage heartbeat;

    private long lastSendTime;

    private boolean aliveChecked = true;

    private ScheduledFuture<?> timeoutTask;

    /**
     * 心跳机制, 确认存活
     */
    private ScheduledFuture<?> aliveChecker;

    private volatile boolean closed = false;

    boolean isConnected() {
        return channel.isConnected();
    }

    public ConnectionSession(@NotNull SocketChannel channel) {
        this.channel = channel;
        heartbeat = HeartbeatMessage.newBeat(channel.socket());
        if (!auth()) {
            close();
        }
        log.debug("new session {}",this);
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        if (database == null) {
            close();
        } else {
            database.restrain(this);
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
        } catch (IOException ignores) {
        }
        aliveChecker.cancel(true);
        if (timeoutTask != null) {
            timeoutTask.cancel(true);
        }
        if (database != null) {
            database.release(this);
        }
        closed = true;
        log.debug("close session {}", this);
    }

    public void startCheckAlive() {
        aliveChecker = AsyncService.setInterval(() -> {
            if (System.currentTimeMillis() - lastSendTime < dbServerConfig.get().getHeartbeatInterval()) {
                if (timeoutTask != null) {
                    timeoutTask.cancel(true);
                }
                return;
            }
            if (!aliveChecked || timeoutTask != null) {
                close();
                return;
            }
            aliveChecked = false;
            timeoutTask = AsyncService.setTimeout(() -> {
                ActHeartbeatMessage act;
                try {
                    act = interact(heartbeat, ActHeartbeatMessage.class);
                } catch (ClosedConnectionException e) {
                    log.error("timeoutTask throw unexpected exception: {}", e.toString());
                    return;
                }
                if (act.ack(heartbeat)) {
                    aliveChecked = true;
                    heartbeat = heartbeat.nextBeat();
                }
                timeoutTask = null;
            }, dbServerConfig.get().getHeartbeatInterval());
        }, dbServerConfig.get().getHeartbeatInterval());
    }

    public <T extends Message> T get(Class<T> messageClass) throws ClosedConnectionException, IllegalMessageException {
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
            throw new RuntimeException(e);
        }
        try {
            Message result = JsonService.parse(new String(Arrays.copyOf(readBuffer.array(), readBuffer.position())), Message.class);
            readBuffer.clear();
            if(result == null) {
                throw new IllegalMessageException("unexpected message type 'null'");
            }
            if(messageClass.isAssignableFrom(result.getClass())) {
                return (T) result;
            }
            throw new IllegalMessageException("unexpected message type '" + MessageType.typeOf(messageClass) + "'");
        } catch (JsonProcessingException e) {
            readBuffer.clear();
            throw new IllegalMessageException("unexpected message type '" + MessageType.typeOf(messageClass) + "'");
        }
    }

    public void send(Message toSend) throws ClosedConnectionException {
        if (closed || !channel.isConnected()) {
            throw new ClosedConnectionException("Connection closed");
        }
        String data = JsonService.stringfy(toSend);
        try {
            channel.write(ByteBuffer.wrap(data.getBytes()));
            lastSendTime = System.currentTimeMillis();
            if (timeoutTask != null) {
                timeoutTask.cancel(true);
                timeoutTask = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends Message> T interact(Message toSend, Class<T> returnMessage) throws ClosedConnectionException {
        send(toSend);
        return get(returnMessage);
    }

    public boolean auth() {
        try {
            AuthenticationMessage authenticationMessage = get(AuthenticationMessage.class);
            // do authentication ...
            send(new ActAuthenticationMessage());
            startCheckAlive();
        } catch (IllegalMessageException e) {
            return false;
        } catch (ClosedConnectionException e) {
            log.error("auth() throw ConnectionClosedException: {}", e.toString());
            throw new DBSystemException(e);
        }
        return true;
    }

    @Override
    public String toString() {
        return "ConnectionSession{" +
                "database=" + database +
                ", id=" + id +
                ", channel=" + channel +
                ", closed=" + closed +
                '}';
    }
}
