package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.UnknownTokenException;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.net.ConnectionSessionModel;
import org.hashdb.ms.net.TransportableConnectionSession;
import org.hashdb.ms.util.JsonService;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Date: 2023/11/24 15:58
 * 编译流, 流式编译一整条命令
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public final class SupplierCompileStream extends DatabaseCompileStream {

    private final Lazy<SupplierCtx> compileResult = Lazy.empty();

    SupplierCompileStream(ConnectionSessionModel session, String command) {
        super(session, command);
    }

    SupplierCompileStream(ConnectionSessionModel session, String @NotNull [] childTokens, DatabaseCompileStream fatherStream, boolean shouldNormalize) {
        super(session, childTokens, fatherStream, shouldNormalize);
    }

    SupplierCompileStream(ConnectionSessionModel session, String[] tokens, DatabaseCompileStream fatherStream) {
        super(session, tokens, fatherStream);
    }

    private SupplierCompileStream(ConnectionSessionModel session, SupplierCtx supplierCtx) {
        super(session);
        compileResult.computedWith(supplierCtx);
    }

    static SupplierCompileStream createWithTransportable(SupplierCtx supplierCtx, TransportableConnectionSession session) {
        return new SupplierCompileStream(session, supplierCtx);
    }

    public static @NotNull SupplierCompileStream open(ConnectionSessionModel session, @NotNull String command) {
        Objects.requireNonNull(session);
        return new SupplierCompileStream(session, command);
    }

    @Override
    public SupplierCtx compile() throws UnknownTokenException {
        if (compileResult.isCached()) {
            return compileResult.get();
        }
        var commandContext = SupplierKeyword.createCtx(token());
        if (commandContext == null) {
            throw new UnknownTokenException("unknown keyword: '" + token() + "'");
        }
        return compile(commandContext);
    }

    public SupplierCtx compile(SupplierCtx mainStatement) throws UnknownTokenException {
        if (compileResult.isCached()) {
            return compileResult.get();
        }
        next();
        mainStatement.compileWithStream(this);
        compileResult.computedWith(mainStatement);
        return mainStatement;
    }

    @Override
    public String run() {
        // 提交， 进入执行时
        Object result = session.getDatabase().submitOpsTaskSync(compile().compileResult());
        return toString(result);
    }

    @Override
    public String runWithExecutor() {
        // 直接使用已生成的编译上下文
        // 直接提交， 进入执行时
        Object result = session.getDatabase().submitOpsTaskSync(compileResult.get().executeWithStream(this));
        return toString(result);
    }

    String toString(Object result) {
        if (result instanceof Boolean ok) {
            return ok ? "\"SUCC\"" : "\"FAIL\"";
        }
        Object normalizeValue = CompileStream.normalizeValue(result);
        return JsonService.stringfy(normalizeValue == null ? "null" : normalizeValue);
    }
}
