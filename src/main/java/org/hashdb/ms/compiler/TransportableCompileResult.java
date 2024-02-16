package org.hashdb.ms.compiler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.net.TransportableConnectionSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class TransportableCompileResult implements CommandExecutor {
    /**
     * 接收端要设置
     */
    @JsonIgnore
    private TransportableConnectionSession session;
    @Getter
    @JsonProperty
    private boolean write;

    /**
     * 当为空时， 说明发送端认为接收端已经缓存过该命令， 不需要传输编译结果
     * 让接收端直接使用接收端的命令缓存运行
     */
    @Nullable
    @JsonProperty
    private CompilerNode compilerCtx;
    @NotNull
    @JsonProperty
    private String command;
    /**
     * 不参与网络传输, 如果这个CompileResult是在本地生成, 则不为空
     */
    @Nullable
    @JsonIgnore
    private CompileStream<?> stream;

    /**
     * 给json序列化预留的构造器
     */
    protected TransportableCompileResult() {
    }

    public TransportableCompileResult mountSession(TransportableConnectionSession session) {
        if (this.stream != null) {
            throw new DBSystemException("can not mount session again. existing session " + this.session + "");
        }
        this.session = session;
        return this;
    }

    @Override
    public CompletableFuture<Object> execute(String command) {
        if (session == null) {
            throw new DBSystemException("call mountSession first");
        }
        // TODO: 2024/1/19 这里要实现
        return null;
    }

    /**
     * @param stream       命令编译流
     * @param assumeCached 预测接收端是否缓存了该命令
     */
    protected TransportableCompileResult(@NotNull CompileStream<?> stream, boolean assumeCached) {
        this.write = stream.isWrite();
        if (!assumeCached) {
            compilerCtx = stream.compile();
        }
        this.stream = stream;
        this.command = stream.command();
    }

    /**
     * 使用远端提供的编译结果, 拿到执行结果
     *
     * @param session 远端传输的 连接会话
     */
    public String run(TransportableConnectionSession session) {
        // 如果其它服务器没穿该值, 说明在那个服务器里, 这条命令被缓存了并
        // 假定收到这个这个编译结果的服务器也知道
        if (compilerCtx == null) {
            return LocalCommandExecutor.runWithCache(session, command);
        }
        if (compilerCtx instanceof SupplierCtx supplierCtx) {
            return SupplierCompileStream.createWithTransportable(supplierCtx, session).runWithExecutor();
        }
        if (compilerCtx instanceof SystemCompileCtx<?> systemCompileCtx) {
            return SystemCompileStream.createWithTransportable(systemCompileCtx, session).runWithExecutor();
        }
        throw new DBSystemException("Unknown compiler context type: " + compilerCtx.getClass().getName());
    }

    /**
     * 直接在本地运行, 拿到执行结果
     */
    public String run() {
        if (stream == null) {
            throw new DBSystemException("can not run TransportableCompileResult in receive endpoint");
        }
        return stream.runWithExecutor();
    }
}
