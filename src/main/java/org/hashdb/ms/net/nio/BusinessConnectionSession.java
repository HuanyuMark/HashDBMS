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
import org.hashdb.ms.config.ClusterGroupConfig;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.AbstractConnectionSession;
import org.hashdb.ms.net.Parameter;
import org.hashdb.ms.net.exception.IllegalAccessException;
import org.hashdb.ms.net.exception.ServerClosedException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.net.nio.protocol.ProtocolCodec;
import org.hashdb.ms.util.*;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Date: 2024/1/16 21:15
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class BusinessConnectionSession extends AbstractConnectionSession implements BaseConnectionSession {
    static final LongIdentityGenerator idGenerator = new LongIdentityGenerator(0, Long.MAX_VALUE);

    static final Lazy<ClusterGroupConfig> replicationGroup = Lazy.of(() -> HashDBMSApp.ctx().getBean(ClusterGroupConfig.class));
    @JsonProperty
    private final long id;
    @JsonProperty
    private String username;
    @JsonProperty
    private Protocol protocol = Protocol.HASH_V1;
    @JsonProperty
    private Channel channel;
    final Lazy<AuthenticationHandler> authenticationHandlerLazy = Lazy.of(AuthenticationHandler::new);

    final Lazy<CommandExecuteHandler> commandExecuteHandlerLazy = Lazy.of(CommandExecuteHandler::new);

    final Lazy<ProtocolSwitchingHandler> protocolSwitchingHandlerLazy = Lazy.of(() -> new ProtocolSwitchingHandler(this));

    final static Lazy<SessionUpgradeHandler> sessionUpgradeHandlerLazy = Lazy.of(() -> HashDBMSApp.ctx().getBean(SessionMountedHandler.class).upgradeHandler());

    ScheduledFuture<?> inactiveTimeoutTask;

    public BusinessConnectionSession() {
        this(idGenerator.nextId());
    }

    protected BusinessConnectionSession(long id) {
        this.id = id;
        var commandCacheConfig = HashDBMSApp.dbServerConfig.get().getCommandCache();
        localCommandCache = new CacheMap<>(commandCacheConfig.getAliveDuration(), commandCacheConfig.getCacheSize());
    }

    public long id() {
        return id;
    }

    @Override
    @Nullable
    public String username() {
        return username;
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public void protocol(Protocol protocol) {
        channel.pipeline().get(ProtocolCodec.class).setProtocol(protocol);
        this.protocol = protocol;
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public void startInactive() {
        inactiveTimeoutTask = AsyncService.setTimeout(() -> {
            log.info("session inactive timeout {}", this);
            close(new CloseMessage(0, "session inactive timeout"));
        }, HashDBMSApp.dbServerConfig.get().getInactiveTimeout());
    }

    @Override
    public void stopInactive() {
        if (inactiveTimeoutTask == null) {
            return;
        }
        inactiveTimeoutTask.cancel(true);
        inactiveTimeoutTask = null;
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
        onClose(this);
        channel.close();
    }

    class AuthenticationHandler extends ChannelInboundHandlerAdapter implements NamedChannelHandler {

        private int requestWithNoAuth = 0;

        private ScheduledFuture<?> closeNoAuthSessionTask = getCloseNoAuthSessionTask(30 * 60 * 1000);

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
                // if the pwd in passwordAuth is null
                // or these user is not exist
                // or pwd is not equal
                if (authMsg.body().pwd() == null) {
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
            long actId = msg instanceof Message<?> m_ ? m_.id() : 0;
            var errorMessage = new ErrorMessage(actId, new IllegalAccessException("require authenticate"));
            ctx.write(errorMessage);
        }

        private Runnable pauseCloseNoAuthSessionTask() {
            if (closeNoAuthSessionTask == null) {
                return () -> {
                };
            }
            var remaining = ((int) closeNoAuthSessionTask.getDelay(TimeUnit.MILLISECONDS));
            closeNoAuthSessionTask.cancel(true);
            return () -> closeNoAuthSessionTask = getCloseNoAuthSessionTask(remaining);
        }

        private void doAuth(ChannelHandlerContext ctx, AuthenticationMessage authMsg) {
            Database userDb = HashDBMSApp.dbSystem.get().getDatabase("user");
            Runnable restart = pauseCloseNoAuthSessionTask();
            userDb.submitOpsTask(OpsTask.of(() -> userDb.get(authMsg.body().uname())))
                    .thenAcceptAsync(hValue -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> user = (Map<String, String>) HValue.unwrapData(hValue);
                        if (user == null || !user.get("pwd").equals(authMsg.body().pwd())) {
                            restart.run();
                            sendAuthFailedMsg(authMsg, ctx);
                            return;
                        }

                        // auth pass
                        BusinessConnectionSession.this.username = authMsg.body().uname();
                        if (closeNoAuthSessionTask != null) {
                            closeNoAuthSessionTask.cancel(true);
                            closeNoAuthSessionTask = null;
                        }
                        var act = new ActAuthenticationMessage(authMsg.id(), true, "SUCC", authMsg.body().uname());
                        log.info("act msg {}", act.body());
                        ctx.write(act);
                        // 通知session状态
                        ctx.write(new SessionStateMessage(BusinessConnectionSession.this));
                        // in the synchronized thread, we should notify nio thread to flush buffer
                        ctx.flush();
                    });
        }

        private void sendAuthFailedMsg(AuthenticationMessage request, ChannelHandlerContext ctx) {
            ErrorMessage msg = new ErrorMessage(request, new IllegalAccessException("incorrect uname or pwd"));
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
            // 处理用户手动的心跳指令
            if ("PING".equalsIgnoreCase(msg.body())) {
                ctx.write(new ActAppCommandMessage(msg, "PONG"));
                return;
            }

            CompileStream<?> compileResult;
            try {
                compileResult = commandExecutor.compile(msg.body());
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
            if (replicationGroup.get().isMaster() || replicationGroup.get().isSlave() && !compileResult.isWrite()) {
                compileResult.execute().handleAsync((result, e) -> {
                    Object ret;
                    if (e == null) {
                        ret = result;
                        var channelFuture = ctx.writeAndFlush(new ActAppCommandMessage(msg, result));
                        channelFuture.addListener(r -> {
                            if (r.isSuccess()) {
                                return;
                            }
                            log.error("write command execution msg error", r.cause());
                        });
                    } else {
                        ret = e;
                        ErrorMessage errorMessage;
                        // 处理执行线程扔出来的异常
                        if (e instanceof DBClientException dbClientException) {
                            errorMessage = new ErrorMessage(msg, dbClientException);
                        } else {
                            log.error("execute command execution error", e);
                            errorMessage = new ErrorMessage(msg, "db internal error");
                        }
                        // write and notify nio thread to send
                        ctx.writeAndFlush(errorMessage);
                    }

                    if (closeLocker != null && executingCount.decrementAndGet() <= 0) {
                        closeLocker.notifyAll();
                    }
                    return ret;
                }, AsyncService.service());
                return;
            }
            // replication
            // expect SLAVE identity
            if (compileResult.isWrite()) {
                //todo 上传写指令
            }
        }

        public void close() {
            if (closeLocker != null) {
                return;
            }
            closeLocker = new Object();
            if (executingCount.get() <= 0) {
                return;
            }
            try {
                closeLocker.wait();
            } catch (InterruptedException ignore) {
                log.warn("command executor is closing rudely");
            }
        }

    }

    @Override
    public void onChannelChange(Channel channel) {
        this.channel = channel;
        var authenticationHandler = authenticationHandlerLazy.get();
        var commandExecuteHandler = commandExecuteHandlerLazy.get();
        var protocolSwitchingHandler = protocolSwitchingHandlerLazy.get();
        var sessionUpgradeHandler = sessionUpgradeHandlerLazy.get();
        var pipeline = channel.pipeline();
        var incorporator = UncaughtExceptionLogger.extract(pipeline);
        pipeline
                .addLast(authenticationHandler.handlerName(), authenticationHandler)
                .addLast(sessionUpgradeHandler.handlerName(), sessionUpgradeHandler)
                .addLast(commandExecuteHandler.handlerName(), commandExecuteHandler)
                .addLast(protocolSwitchingHandler.handlerName(), protocolSwitchingHandler);

        incorporator.incorporate();
        protocol(protocol);
    }

    public void disableCommandExecuteHandler() {
        channel.pipeline().remove(CommandExecuteHandler.class);
        var commandExecuteHandler = commandExecuteHandlerLazy.get();
        commandExecuteHandler.close();
    }

    @Override
    public SessionMeta getMeta() {
        return SessionMeta.BUSINESS;
    }

    @Override
    public String toString() {
        return STR."Session[\{getMeta()}] \{JsonService.toString(this)}";
    }

    public static final BusinessConnectionSession DEFAULT = new BusinessConnectionSession() {
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
        public void close() {
        }

        @Override
        public void close(CloseMessage closeMessage) {
        }

        @Override
        public void startInactive() {
        }

        @Override
        public void stopInactive() {
        }

        @Override
        public void onChannelChange(Channel channel) {
        }
    };
}
