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

public class TransportableCompileResult {

    @Getter
    @JsonProperty
    private boolean write;

    @Nullable
    @JsonProperty
    private CompilerNode compilerCtx;
    @Nullable
    @JsonProperty
    private String cachedCommand;
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

    protected TransportableCompileResult(@NotNull CompileStream<?> stream) {
        this.write = stream.isWrite();
        compilerCtx = stream.compile();
        this.stream = stream;
    }

    /**
     * @param cachedCommand 当前服务器下被缓存的命令
     */
    protected TransportableCompileResult(String cachedCommand, CompileStream<?> stream) {
        this.write = stream.isWrite();
        this.stream = stream;
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
            if (cachedCommand == null) {
                throw new DBSystemException("cached command is required");
            }
            return CommandExecutor.runWithCache(session, cachedCommand);
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
