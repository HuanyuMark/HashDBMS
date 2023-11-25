package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.exception.CommandCompileException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Date: 2023/11/25 12:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class ConsumerCompileStream extends TokenCompileStream {

    private DataType opsTargetDataType;
    private final CompileCtx<?> fatherCompileCtx;

    protected ConsumerCompileStream(Database database,
                                    String @NotNull [] childTokens,
                                    TokenCompileStream fatherSteam,
                                    CompileCtx<?> fatherCompileCtx) {
        super(database, childTokens, fatherSteam);
        Objects.requireNonNull(fatherCompileCtx);
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    public ConsumerCtx<?> compile() {
        var cmdCtx = ConsumerKeyword.createCtx(token(), fatherCompileCtx);
        if (cmdCtx == null) {
            throw new CommandCompileException("unknown keyword: '" + token() + "'");
        }
        next();
        cmdCtx.compileWithStream(this);
        return cmdCtx;
    }
}
