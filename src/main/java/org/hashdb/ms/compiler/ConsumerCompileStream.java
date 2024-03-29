package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.net.ConnectionSession;
import org.hashdb.ms.util.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Date: 2023/11/25 12:55
 *
 * @author Huanyu Mark
 */
@Slf4j
public final class ConsumerCompileStream extends DatabaseCompileStream {
    private final Lazy<ConsumerCtx<?>> compileResult = Lazy.empty();

    private final CompileCtx<?> fatherCompileCtx;

    ConsumerCompileStream(ConnectionSession session,
                          String @NotNull [] childTokens,
                          DatabaseCompileStream fatherSteam,
                          CompileCtx<?> fatherCompileCtx) {
        super(session, childTokens, fatherSteam);
        Objects.requireNonNull(fatherCompileCtx);
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    public ConsumerCtx<?> compile() {
        if (compileResult.isResolved()) {
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

    @Override
    public CompletableFuture<Object> execute() {
        //todo 实现
        return null;
    }
}
