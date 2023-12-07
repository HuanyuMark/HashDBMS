package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Date: 2023/11/25 12:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public final class ConsumerCompileStream extends DatabaseCompileStream {
    private final Lazy<ConsumerCtx<?>> compileResult = Lazy.of(null);

    private final CompileCtx<?> fatherCompileCtx;

    ConsumerCompileStream(Database database,
                          String @NotNull [] childTokens,
                          DatabaseCompileStream fatherSteam,
                          CompileCtx<?> fatherCompileCtx) {
        super(database, childTokens, fatherSteam);
        Objects.requireNonNull(fatherCompileCtx);
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    public ConsumerCtx<?> compile() {
        if (compileResult.isCached()) {
            return compileResult.get();
        }
        var cmdCtx = ConsumerKeyword.createCtx(token(), fatherCompileCtx);
        if (cmdCtx == null) {
            throw new CommandCompileException("unknown keyword: '" + token() + "'");
        }
        next();
        cmdCtx.compileWithStream(this);
        compileResult.computedWith(cmdCtx);
        return cmdCtx;
    }
}
