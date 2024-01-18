package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.CommandExecutor;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.net.AbstractConnectionSession;
import org.hashdb.ms.net.exception.IllegalAccessException;
import org.hashdb.ms.net.nio.msg.v1.*;
import org.hashdb.ms.net.nio.protocol.ProtocolCodec;
import org.hashdb.ms.util.AsyncService;
import org.hashdb.ms.util.CacheMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Date: 2024/1/16 21:15
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class NettyConnectionSession extends AbstractConnectionSession {
    public static final AttributeKey<NettyConnectionSession> KEY = AttributeKey.newInstance("session");

    private static final AtomicLong idGenerator = new AtomicLong(0);

    private final long id = idGenerator.incrementAndGet();
    private String username;

    private ProtocolCodec supportedProtocolCodec = ProtocolCodec.HASH_V1;
    private Channel channel;

    private Consumer<NettyConnectionSession> onClose;

    private AuthenticationMessageHandler authenticationMessageHandler;

    private CommandExecuteHandler commandExecuteHandler;

    public NettyConnectionSession(Channel channel, Consumer<NettyConnectionSession> onClose) {
        this.channel = channel;
        this.onClose = onClose;
        localCommandCache = new CacheMap<>(dbServerConfig.get().getCommandCache().getAliveDuration(), dbServerConfig.get().getCommandCache().getCacheSize());
    }

    public NettyConnectionSession onClose(Consumer<NettyConnectionSession> onClose) {
        this.onClose = onClose;
        return this;
    }

    public long id() {
        return id;
    }

    public Channel channel(Channel channel) {
        Channel old = this.channel;
        this.channel = channel;
        return old;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (getDatabase() != null) {
            getDatabase().release();
        }
        onClose.accept(this);
        channel.close();
    }

    public synchronized void close(CloseMessage closeMessage) {
        if (closed) {
            return;
        }
        if (getDatabase() != null) {
            getDatabase().release();
        }
        if (closeMessage != null) {
            channel.writeAndFlush(closeMessage);
        }
    }

    @Override
    public synchronized void close(org.hashdb.ms.net.client.CloseMessage closeMessage) {
        close(new CloseMessage(closeMessage.getData()));
    }

    @Nullable
    public String username() {
        return username;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void supportedProtocol(ProtocolCodec protocolCodec) {
        this.supportedProtocolCodec = protocolCodec;
    }

    public ProtocolCodec supportedProtocol() {
        return supportedProtocolCodec;
    }

    ChannelInboundHandler authorizeHandler() {
        return authenticationMessageHandler == null ? (authenticationMessageHandler = new AuthenticationMessageHandler()) : authenticationMessageHandler;
    }

    CommandExecuteHandler commandExecuteHandler() {
        return commandExecuteHandler == null ? (commandExecuteHandler = new CommandExecuteHandler()) : commandExecuteHandler;
    }

    class AuthenticationMessageHandler extends ChannelInboundHandlerAdapter {

        private int requestWithNoAuth = 0;

        private ScheduledFuture<?> closeNoAuthSessionTask = getCloseNoAuthSessionTask(30 * 60 * 1000);

        private ScheduledFuture<?> getCloseNoAuthSessionTask(int timeout) {
            return AsyncService.setTimeout(() -> {
                if (username != null) {
                    return;
                }
                CloseMessage closeMessage = new CloseMessage("\"authentication timeout\"");
                close(closeMessage);
            }, timeout);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // if auth is passed
            if (username != null || msg instanceof CloseMessage) {
                ctx.fireChannelRead(msg);
                return;
            }
            if (!(msg instanceof AuthenticationMessage authMsg)) {
                if (requestWithNoAuth++ > 3) {
                    var closeMessage = new CloseMessage("\"require authenticate\"");
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
                var errorMessage = new ErrorMessage(new IllegalAccessException("require authenticate"));
                ctx.writeAndFlush(errorMessage);
                return;
            }

            // start auth
            // if the password in passwordAuth is null
            // or these user is not exist
            // or password is not equal
            if (authMsg.body().password() == null) {
                sendAuthFailedMsg(ctx);
                return;
            }
            Database userDb = dbSystem.get().getDatabase("user");
            @SuppressWarnings("unchecked")
            Map<String, String> user = (Map<String, String>) HValue.unwrapData(userDb.submitOpsTaskSync(OpsTask.of(() -> userDb.get(authMsg.body().username()))));
            if (user == null || !user.get("password").equals(authMsg.body().password())) {
                sendAuthFailedMsg(ctx);
                return;
            }

            // auth pass
            NettyConnectionSession.this.username = authMsg.body().username();
            if (closeNoAuthSessionTask != null) {
                closeNoAuthSessionTask.cancel(true);
                closeNoAuthSessionTask = null;
            }
            var act = new ActAuthenticationMessage(authMsg.id(), true, "\"SUCC\"", authMsg.body().username());
            ctx.writeAndFlush(act);
//                    startCheckAlive();
        }

        private static void sendAuthFailedMsg(ChannelHandlerContext ctx) {
            ErrorMessage msg = new ErrorMessage(new IllegalAccessException("incorrect username or password"));
            ctx.writeAndFlush(msg);
        }
    }

    class CommandExecuteHandler extends ChannelInboundHandlerAdapter {

        private final CommandExecutor commandExecutor = CommandExecutor.create(NettyConnectionSession.this);

        public void channelRead(ChannelHandlerContext ctx, Object m) {
            if (!(m instanceof AppCommandMessage appCommandMessage)) {
                ctx.fireChannelRead(m);
                return;
            }
            var actId = appCommandMessage.id();
            var future = commandExecutor.execute(appCommandMessage.body());
            future.handleAsync((result, e) -> {
                if (e == null) {
                    ctx.fireChannelRead(new ActAppCommandMessage(result).actId(actId));
                    return result;
                }
                if (!(e instanceof DBClientException dbClientException)) {
                    log.error("execute command internal error", e);
                    return e;
                }
                ctx.fireChannelRead(new ErrorMessage(dbClientException));
                return e;
            }, AsyncService.service());
        }
    }
}
