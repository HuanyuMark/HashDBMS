package org.hashdb.ms.net.nio;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DefaultConfig;
import org.hashdb.ms.net.exception.AuthenticationFailedException;
import org.hashdb.ms.net.nio.msg.v1.ActAuthenticationMessage;
import org.hashdb.ms.net.nio.msg.v1.AuthenticationMessage;
import org.hashdb.ms.net.nio.msg.v1.ErrorMessage;
import org.hashdb.ms.net.nio.protocol.Protocol;
import org.hashdb.ms.net.nio.protocol.ProtocolCodec;
import org.hashdb.ms.support.Checker;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.util.AsyncService;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Date: 2023/12/5 13:47
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
public class ServerNode {
    private static final EventLoopGroup serverNodeEventLoopGroup = new NioEventLoopGroup();
    /**
     * 主机给从结点分配了id后, 会广播已分配的最大分布式ID, 所有从节点都要同步.
     * 从节点同步后, 都必须要响应这个消息, 以表示已同步完成(确保所有从机在切换为主机后,都能继续正确地分配分布式ID).
     * 否则, 主机不能分配下一个分布式ID, 如果主机等待的这个从机过久(该从机超时未返回同步成功的消息)
     * 则走从机宕机流程, 由集群判断这个从机时候时候客观下线(严格确保幂等性)
     */
    private static long maxDistributionId;
    @JsonProperty
    protected final String host;
    @JsonIgnore
    private InetAddress address;
    @JsonProperty
    protected final int port;
    @JsonProperty
    protected final String username;
    @JsonProperty
    protected final String password;
    @JsonIgnore
    protected Channel channel;
    @JsonIgnore
    private String key;

    private int hashcode;
    //    private List<Runnable> onDistributionIdChangeCallbacks = new ArrayList<>();
    private Consumer<ServerNode> onDistributionIdChangeCallback;

    private Consumer<ServerNode> onSubjectiveFailCallback;

    /**
     * 主观下线标志位
     */
    protected boolean online;

    /**
     * 客观下线(故障)标志位
     */
    protected boolean breakdown;

    private final ActMessageHandler actMessageHandler = new ActMessageHandler();

    class ServerNodeChannelInitializer extends ChannelInitializer<NioSocketChannel> {
        @Override
        protected void initChannel(NioSocketChannel ch) {
            var codec = new ProtocolCodec(Protocol.HASH_V1);
            var heartbeatHandler = new HeartbeatHandler() {
                @Override
                protected void closeChannel(Channel channel) {
                    online = false;
                    doSubjectiveFail(channel);
                }
            };
            ch.pipeline()
                    .addLast(heartbeatHandler.handlerName(), heartbeatHandler)
                    .addLast(codec.handlerName(), codec)
                    .addLast(actMessageHandler.handlerName(), actMessageHandler)
            ;
            ServerNode.this.initChannel(ch);
        }
    }

    public ActMessageHandler actMessageHandler() {
        return actMessageHandler;
    }

    @ConstructorBinding
    public ServerNode(
            String host,
            String ip,
            Integer port,
            String username,
            String password,
            String uname,
            String pwd
    ) {
        if (port == null) {
            throw Exit.error(STR."invalid server node. {host:\{host}, port:null}", "port is required");
        }
        if (port > 65535 || port < 0) {
            throw Exit.error(STR."invalid server node. {host:\{host}, port:\{port}", "port is out of range [0,65535]");
        }
        this.host = Checker.require("illegal host", "host/ip", host, ip);
        AsyncService.start(() -> {
            try {
                address = InetAddress.getByName(this.host);
            } catch (UnknownHostException e) {
                throw Exit.error(STR."unresolvable host: '\{this.host}'.", e);
            }
        });
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * 如果还没有连接该node, 则发起连接并验证后, 如果验证成功后, 返回
     * <p>
     * 可能会扔出 {@link TimeoutException}.
     * {@link AuthenticationFailedException }
     */
    public Promise<Channel> channel() {
        var promise = new DefaultPromise<Channel>(serverNodeEventLoopGroup.next());
        if (channel == null || !channel.isActive()) {
            newChannel().handleAsync((channel, e) -> {
                if (e != null) {
                    promise.setFailure(e);
                    return null;
                }
                this.channel = channel;
                this.online = true;
                promise.setSuccess(channel);
                return null;
            }, AsyncService.service());
            return promise;
        }
        promise.setSuccess(channel);
        return promise;
    }

    protected CompletableFuture<Channel> newChannel() {
        if (channel != null) {
            channel.disconnect();
        }
        var client = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(serverNodeEventLoopGroup)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, channelConnectTimeout())
                .handler(new ServerNodeChannelInitializer());
        ChannelFuture future;
        if (address == null) {
            future = client.connect(host, port);
        } else {
            future = client.connect(address, port);
        }
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        var timeoutTask = AsyncService.setTimeout(() -> {
            completableFuture.completeExceptionally(new TimeoutException());
        }, 10_000);
        future.addListener(res -> {
            if (res.isSuccess()) {
                doAuth(future.channel(), completableFuture);
            } else {
                completableFuture.completeExceptionally(res.cause());
            }
            if (address == null) {
                try {
                    address = InetAddress.getByName(host);
                } catch (UnknownHostException e) {
                    throw Exit.error(STR."expect resolvable host, but can not resolve. host: '\{host}'", e);
                }
            }
        });
        return completableFuture;
    }

    private void doAuth(Channel channel, CompletableFuture<Channel> future) {
        var actMessageHandler = ActMessageHandler.get(channel);
        String username;
        String password;
        var defaultConfig = HashDBMSApp.ctx().getBean(DefaultConfig.class);
        var remoteConfig = defaultConfig.getAuth();
        if (this.username == null) {
            if (remoteConfig.username() == null) {
                future.completeExceptionally(new AuthenticationFailedException("username is required"));
                return;
            }
            username = remoteConfig.username();
        } else {
            username = this.username;
        }
        if (this.password == null) {
            if (remoteConfig.password() == null) {
                future.completeExceptionally(new AuthenticationFailedException("password is required"));
                return;
            }
            password = remoteConfig.password();
        } else {
            password = this.password;
        }
        var request = actMessageHandler.newRequest(new AuthenticationMessage(username, password));
        request.apply().handleAsync((msg, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
                return e;
            }
            if (msg instanceof ActAuthenticationMessage actAuthenticationMessage) {
                if (actAuthenticationMessage.body().success()) {
                    future.complete(channel);
                } else {
                    future.completeExceptionally(new AuthenticationFailedException(actAuthenticationMessage.body().msg()));
                }
                return msg;
            }
            if (msg instanceof ErrorMessage errorMessage) {
                future.completeExceptionally(errorMessage.toException());
            }
            return msg;
        }, AsyncService.service());
    }

    protected long recoverCheckInterval() {
        return HashDBMSApp.ctx().getBean(DefaultConfig.class).getRecoverCheckInterval();
    }

    protected int channelConnectTimeout() {
        return HashDBMSApp.ctx().getBean(DefaultConfig.class).getConnectTimeoutMillis();
    }

    /**
     * @return 如果检查到这个结点恢复了, 则Promise返回验证成功的Channel, 否则, 一直阻塞
     */
    protected Promise<Channel> recover() {
        if (channel != null && channel.isActive()) {
            throw new IllegalStateException("this server node is active");
        }
        var promise = new DefaultPromise<Channel>(serverNodeEventLoopGroup.next());
        var poller = AsyncService.setInterval(() -> {
            InetAddress address;
            if (this.address != null) {
                address = this.address;
            } else {
                try {
                    AsyncService.waitTimeout(() -> {
                        try {
                            this.address = InetAddress.getByName(host);
                        } catch (UnknownHostException e) {
                            throw Exit.error(STR."unresolvable host '\{host}'", e);
                        }
                    }, ((long) Math.max(recoverCheckInterval() * 0.3, 1)));
                    address = this.address;
                } catch (TimeoutException e) {
                    throw Exit.error(STR."unresolvable host '\{host}'", e);
                }
            }
            try {
                if (!address.isReachable(1_000)) {
                    return;
                }
            } catch (IOException ignore) {
                return;
            }
            channel().addListener(res -> {
                if (res.isSuccess()) {
                    promise.setSuccess((Channel) res.getNow());
                } else {
                    log.warn("recover poller throw unexpected error:", res.cause());
                    // 不改变promise状态. 重试到成功连接为止
                }
            });
        }, recoverCheckInterval());
        promise.addListener(res -> {
            // 这个promise只允许成功
            poller.cancel(false);
        });
        return promise;
    }

    /**
     * @param channel 连接该Node, 绑定一堆相关handler
     */
    protected void initChannel(NioSocketChannel channel) {
    }

    /**
     * @param channel 主管下线的channel
     */
    protected void doSubjectiveFail(Channel channel) {
        if (onSubjectiveFailCallback == null) {
            channel.close();
        } else {
            onSubjectiveFailCallback.accept(this);
        }
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String key() {
        return key == null ? (key = STR."\{host}:\{port}") : key;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public void onDistributionIdChange(Consumer<ServerNode> cb) {
        if (onDistributionIdChangeCallback != null) {
            throw new IllegalStateException("callback already set");
        }
        onDistributionIdChangeCallback = cb;
    }

    public void onSubjectiveFail(Consumer<ServerNode> cb) {
        if (onSubjectiveFailCallback != null) {
            throw new IllegalStateException("callback already set");
        }
        onSubjectiveFailCallback = cb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerNode that)) return false;
        if (port != that.port) return false;
        return Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        if (hashcode != 0) {
            return hashcode;
        }
        hashcode = host.hashCode();
        hashcode = 31 * hashcode + port;
        return hashcode;
    }
}
