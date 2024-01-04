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

public class TransportableCompileResult {

    @Getter
    @JsonProperty
    private boolean write;

    @JsonProperty
    private CompilerNode compilerCtx;


    /**
     * 不参与网络传输, 如果这个CompileResult是在本地生成, 则不为空
     */
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
     * 使用远端提供的编译结果, 拿到执行结果
     *
     * @param session 远端传输的 连接会话
     */
    public String run(TransportableConnectionSession session) {
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
        return stream.runWithExecutor();
    }
}
