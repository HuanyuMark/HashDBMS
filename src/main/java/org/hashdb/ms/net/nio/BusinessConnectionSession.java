package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.compiler.CompileStream;
import org.hashdb.ms.compiler.LocalCommandExecutor;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.AbstractConnectionSession;
import org.hashdb.ms.net.Parameter;
import org.hashdb.ms.net.exception.IllegalAccessException;
import org.hashdb.ms.net.exception.MaxConnectionException;
import org.hashdb.ms.net.exception.ServerClosedException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.util.*;
import org.jetbrains.annotations.Nullable;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/1/16 21:15
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class BusinessConnectionSession extends AbstractConnectionSession implements BaseConnectionSession {
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    static final LongIdentityGenerator idGenerator = new LongIdentityGenerator(1, Long.MAX_VALUE);
    @JsonProperty
    final long id;
    @JsonProperty
    String username;
    @JsonProperty
    Protocol protocol = Protocol.HASH_V1;
    @JsonProperty
    Channel channel;
    final Lazy<AuthenticationHandler> authenticationHandlerLazy = Lazy.of(AuthenticationHandler::new);

    final Lazy<CommandExecuteHandler> commandExecuteHandlerLazy = Lazy.of(CommandExecuteHandler::new);

    final Lazy<ProtocolSwitchingHandler> protocolSwitchingHandlerLazy = Lazy.of(() -> new ProtocolSwitchingHandler(this));

    final static Lazy<SessionUpgradeHandler> sessionUpgradeHandlerLazy = Lazy.of(()-> HashDBMSApp.ctx().getBean(SessionMountedHandler.class).upgradeHandler());

    ScheduledFuture<?> inactiveTimeoutTask;

    @Override
    public void onClose(TransientConnectionSession session) {
    }

    @Override
    public void close() {
        close((CloseMessage) null);
    }

    @Override
    public void close(@Nullable CloseMessage closeMessage) {
        if (closed) {
            return;
        }
        // 等待所有命令执行完毕
        if (commandExecuteHandlerLazy.isResolved()) {
            commandExecuteHandlerLazy.get().close();
        }
        if (getDatabase() != null) {
            getDatabase().release();
        }
        if (closeMessage != null && channel.isActive()) {
            channel.write(closeMessage);
        }
        connectionCount.decrementAndGet();
        onClose(this);
        channel.close();
    }

    @Override
    @Nullable
    public String username() {
        return username;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public void protocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public void startInactive() {
        authenticationHandlerLazy.get().heartbeatHandler.stopHeartbeat();
        inactiveTimeoutTask = AsyncService.setTimeout(() -> {
            log.info("session inactive timeout {}", this);
            close(new CloseMessage(0, "session inactive timeout"));
        }, dbServerConfig.get().getInactiveTimeout());
    }

    @Override
    public void stopInactive() {
        if (inactiveTimeoutTask == null) {
            return;
        }
        authenticationHandlerLazy.get().heartbeatHandler.startHeartbeat();
        inactiveTimeoutTask.cancel(true);
        inactiveTimeoutTask = null;
    }
    class AuthenticationHandler extends ChannelInboundHandlerAdapter implements NamedChannelHandler {

        private int requestWithNoAuth = 0;

        private ScheduledFuture<?> closeNoAuthSessionTask = getCloseNoAuthSessionTask(30 * 60 * 1000);

        private HeartbeatHandler heartbeatHandler;

        private ScheduledFuture<?> getCloseNoAuthSessionTask(int timeout) {
            return AsyncService.setTimeout(() -> {
                if (username != null) {
                    return;
                }
                CloseMessage closeMessage = new CloseMessage(0, "authentication timeout");
                close(closeMessage);
            }, timeout);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            log.info("auth {}", msg);
            // if auth is passed
            if (msg instanceof AuthenticationMessage authMsg) {
                log.info("query user with {}", authMsg.body());

                // start auth
                // if the password in passwordAuth is null
                // or these user is not exist
                // or password is not equal
                if (authMsg.body().password() == null) {
                    sendAuthFailedMsg(authMsg, ctx);
                    return;
                }
                doAuth(ctx, authMsg);
                return;
            }
            if (username != null || msg instanceof CloseMessage) {
                ctx.fireChannelRead(msg);
                return;
            }
            if (requestWithNoAuth++ > 3) {
                var closeMessage = new CloseMessage(0, "require authenticate");
                if (closeNoAuthSessionTask != null) {
                    closeNoAuthSessionTask.cancel(true);
                }
                close(closeMessage);
                return;
            }
            if (closeNoAuthSessionTask != null) {
                closeNoAuthSessionTask.cancel(true);
                closeNoAuthSessionTask = getCloseNoAuthSessionTask(3000);
            }
            var errorMessage = new ErrorMessage(msg instanceof Message<?> m_ ? m_.id() : 0, new IllegalAccessException("require authenticate"));
            ctx.write(errorMessage);
        }

        protected void startCheckHeartbeat(Channel channel) {
            if (heartbeatHandler == null) {
                heartbeatHandler = new HeartbeatHandler(BusinessConnectionSession.this);
            }
            channel.pipeline().addAfter(handlerName(), heartbeatHandler.handlerName(), heartbeatHandler);
            heartbeatHandler.startHeartbeat();
        }

        private Runnable pauseCloseNoAuthSessionTask() {
            if (closeNoAuthSessionTask == null) {
                return () -> {
                };
            }
            closeNoAuthSessionTask.cancel(true);
            var remaining = ((int) closeNoAuthSessionTask.getDelay(TimeUnit.MILLISECONDS));
            return () -> closeNoAuthSessionTask = getCloseNoAuthSessionTask(remaining);
        }

        private void doAuth(ChannelHandlerContext ctx, AuthenticationMessage authMsg) {
            Database userDb = dbSystem.get().getDatabase("user");
            Runnable restart = pauseCloseNoAuthSessionTask();
            userDb.submitOpsTask(OpsTask.of(() -> userDb.get(authMsg.body().username())))
                    .thenAcceptAsync(hValue -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> user = (Map<String, String>) HValue.unwrapData(hValue);
                        if (user == null || !user.get("password").equals(authMsg.body().password())) {
                            restart.run();
                            sendAuthFailedMsg(authMsg, ctx);
                            return;
                        }

                        // auth pass
                        BusinessConnectionSession.this.username = authMsg.body().username();
                        if (closeNoAuthSessionTask != null) {
                            closeNoAuthSessionTask.cancel(true);
                            closeNoAuthSessionTask = null;
                        }
                        var act = new ActAuthenticationMessage(authMsg.id(), true, "SUCC", authMsg.body().username());
                        log.info("act msg {}", act.body());
                        ctx.write(act);
                        // 通知session状态
                        ctx.write(new SessionStateMessage(BusinessConnectionSession.this));
                        // in the synchronized thread, we should notify nio thread to flush buffer
                        ctx.flush();
                        startCheckHeartbeat(ctx.channel());
                    });
        }

        private void sendAuthFailedMsg(AuthenticationMessage request, ChannelHandlerContext ctx) {
            ErrorMessage msg = new ErrorMessage(request, new IllegalAccessException("incorrect username or password"));
            ctx.write(msg);
        }
    }

    class CommandExecuteHandler extends SimpleChannelInboundHandler<AppCommandMessage> implements Closeable, NamedChannelHandler {

        private final LocalCommandExecutor commandExecutor = LocalCommandExecutor.create(BusinessConnectionSession.this);

        private final AtomicInteger executingCount = new AtomicInteger(0);

        private Object closeLocker;
        private DBClientException serverClosedException;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, AppCommandMessage msg) {
            if (closeLocker != null) {
                ctx.write((serverClosedException == null ? (serverClosedException = new ServerClosedException("db server is closing")) : serverClosedException).msg(msg.id()));
                return;
            }
            CompletableFuture<?> exeResult;
            try {
                exeResult = commandExecutor.execute(msg.body());
            } catch (DBClientException e) {
                // 捕获编译期异常
                ctx.write(new ErrorMessage(msg, e));
                return;
            } catch (Exception e) {
                log.error("uncaught command compile error", e);
                ctx.write(new ErrorMessage(msg, "db internal error"));
                return;
            }
            executingCount.incrementAndGet();
            exeResult.handleAsync((result, e) -> {
                int finishedCount = executingCount.decrementAndGet();
                if (closeLocker != null && finishedCount <= 0) {
                    closeLocker.notifyAll();
                }
                if (e == null) {
                    ctx.writeAndFlush(new ActAppCommandMessage(msg, result));
                    return result;
                }
                // 处理执行线程扔出来的异常
                if (!(e instanceof DBClientException dbClientException)) {
                    log.error("execute command execution error", e);
                    ctx.writeAndFlush(new ErrorMessage(msg, "db internal error"));
                    return e;
                }
                // write and notify nio thread to send
                ctx.writeAndFlush(new ErrorMessage(msg, dbClientException));
                return e;
            }, AsyncService.service());
        }

        public void close() {
            if (closeLocker != null) {
                throw new IllegalStateException("already closed");
            }
            closeLocker = new Object();
            if (executingCount.get() <= 0) {
                return;
            }
            try {
                closeLocker.wait();
            } catch (InterruptedException ignore) {
            }
        }

    }
    protected BusinessConnectionSession(long id) {
        this.id = id;
        var config = dbServerConfig.get();
        if (connectionCount.incrementAndGet() > config.getMaxConnections()) {
            connectionCount.decrementAndGet();
            throw new MaxConnectionException("too many connections");
        }
        var commandCacheConfig = config.getCommandCache();
        localCommandCache = new CacheMap<>(commandCacheConfig.getAliveDuration(), commandCacheConfig.getCacheSize());
    }

    public BusinessConnectionSession(SessionMountedHandler sessionMountedHandler) {
        this(idGenerator.nextId());
    }


    @Override
    public long id() {
        return id;
    }

    @Override
    public void onChannelChange(Channel channel) {
        this.channel = channel;
        var authenticationHandler = authenticationHandlerLazy.get();
        var commandExecuteHandler = commandExecuteHandlerLazy.get();
        var protocolSwitchingHandler = protocolSwitchingHandlerLazy.get();
        var sessionUpgradeHandler = sessionUpgradeHandlerLazy.get();
        channel.pipeline()
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, authenticationHandler.handlerName(), authenticationHandler)
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, sessionUpgradeHandler.handlerName(), sessionUpgradeHandler)
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, commandExecuteHandler.handlerName(), commandExecuteHandler)
                .addBefore(UncaughtExceptionLogger.HANDLER_NAME, protocolSwitchingHandler.handlerName(), protocolSwitchingHandler);
        if(username == null) {
            return;
        }
        authenticationHandlerLazy.get().startCheckHeartbeat(channel);
    }

    @Override
    public void onReleaseChannel() {
        channel.pipeline().remove(AuthenticationHandler.class);
        try (var commandExecuteHandler = channel().pipeline().remove(CommandExecuteHandler.class)) {
            channel().pipeline().remove()
        }
    }

    @Override
    public SessionMeta getSessionMeta() {
        return SessionMeta.BUSINESS;
    }

    @Override
    public String toString() {
        return "Session " + JsonService.toString(this);
    }

    public static final BusinessConnectionSession DEFAULT = new BusinessConnectionSession(null) {
        @Override
        public CacheMap<String, CompileStream<?>> getLocalCommandCache() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Database getDatabase() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDatabase(Database database) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Parameter setParameter(String name, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Parameter getParameter(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel channel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }

        @Override
        public void close(CloseMessage closeMessage) {
        }

        @Override
        public void close(org.hashdb.ms.net.client.CloseMessage closeMessage) {
        }

        @Override
        public void startInactive() {
        }

        @Override
        public void stopInactive() {
        }

        @Override
        public void onClose(TransientConnectionSession session) {
        }
        @Override
        public void onChannelChange(Channel channel) {
        }

        @Override
        public void onReleaseChannel() {
        }
    };
}
