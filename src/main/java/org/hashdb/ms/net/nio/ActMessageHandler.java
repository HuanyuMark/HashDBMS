package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.nio.msg.v1.ActMessage;
import org.hashdb.ms.net.nio.msg.v1.Message;
import org.hashdb.ms.util.AsyncService;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

/**
 * Date: 2024/2/20 14:45
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
@Slf4j
public class ActMessageHandler extends SimpleChannelInboundHandler<ActMessage<?>> implements NamedChannelHandler {
    private Channel channel;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        clear();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clear();
        ctx.fireChannelInactive();
    }

    private final LinkedHashMap<Integer, RequestContext> responseMap = new LinkedHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ActMessage<?> msg) {
        var reqCtx = responseMap.remove(msg.actId());
        if (reqCtx == null) {
            log.warn("can not found matched request of response '{}'", msg);
            ctx.fireChannelRead(msg);
            return;
        }
        reqCtx.responseFuture.complete(msg);
        if (!reqCtx.intercept()) {
            ctx.fireChannelRead(msg);
        }
    }

    private void clear() {
        channel = null;
        responseMap.clear();
    }

    public static void logRequestException(Throwable e) {
        while (e instanceof CompletionException c) {
            e = c.getCause();
        }
        log.warn("request throw error", e);
    }

    public RequestContext newRequest(Message<?> request) {
        return new RequestContext(request);
    }

    public class RequestContext {

        private final Message<?> request;
        private final CompletableFuture<Message<?>> responseFuture = new CompletableFuture<>();

        /**
         * ms
         */
        private long timeout = 5_000;

        /**
         * 拦截, 是否在 {@link ActMessageHandler} 的读事件方法中匹配了相应结果后
         * 继续将结果交由 后面处理器 处理
         */
        private boolean intercept = true;

        private ScheduledFuture<?> timeoutTask;

        public RequestContext(Message<?> request) {
            this.request = request;
            responseFuture.thenApplyAsync(m -> {
                cancelTimeout();
                clear();
                return m;
            }, AsyncService.service());
        }

        private void doRequest() {
            responseMap.put(request.id(), this);
            channel.writeAndFlush(request);
        }

        private void clear() {
            responseMap.remove(request.id());
        }

        public RequestContext shouldIntercept() {
            this.intercept = true;
            return this;
        }

        public boolean intercept() {
            return intercept;
        }

        public RequestContext timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public CompletableFuture<Message<?>> apply() throws CompletionException {
            doRequest();
            timeoutTask = AsyncService.setTimeout(() -> {
                clear();
                timeoutTask = null;
                responseFuture.completeExceptionally(new TimeoutException());
            }, timeout);
            return responseFuture;
        }

        void cancelTimeout() {
            if (timeoutTask == null) {
                return;
            }
            timeoutTask.cancel(true);
            timeoutTask = null;
        }
    }
}
