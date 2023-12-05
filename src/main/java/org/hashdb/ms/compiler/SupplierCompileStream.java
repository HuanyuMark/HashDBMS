package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.UnknownTokenException;
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

    private final Lazy<SupplierCtx> compileResult = Lazy.of(null);

    SupplierCompileStream(Database database, String command) {
        super(database, command);
    }

    SupplierCompileStream(Database database, String @NotNull [] childTokens, DatabaseCompileStream fatherStream, boolean shouldNormalize) {
        super(database, childTokens, fatherStream, shouldNormalize);
    }

    SupplierCompileStream(Database database, String[] tokens, DatabaseCompileStream fatherStream) {
        super(database, tokens, fatherStream);
    }

    private SupplierCompileStream(Database database, SupplierCtx supplierCtx) {
        super(database);
        compileResult.computedWith(supplierCtx);
    }

    static SupplierCompileStream createWithTransportable(Database database, SupplierCtx supplierCtx) {
        return new SupplierCompileStream(database, supplierCtx);
    }

    public static @NotNull SupplierCompileStream open(Database db, @NotNull String command) {
        Objects.requireNonNull(db);
        return new SupplierCompileStream(db, command);
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
        next();
        commandContext.compileWithStream(this);
        compileResult.computedWith(commandContext);
        return commandContext;
    }

    @Override
    public String run() {
        Object result = database.submitOpsTaskSync(compile().compileResult());
        return toString(result);
    }

    /**
     * 直接使用已生成的 Ctx, 和其执行器, 生成执行结果
     */
    @Override
    public String runWithExecutor() {
        // 直接使用已生成的编译上下文
        Object result = database.submitOpsTaskSync(compileResult.get().executeWithStream(this));
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
