package org.hashdb.ms.compiler;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
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
    private final CmdCtx<?> fatherCmdCtx;

    protected ConsumerCompileStream(Database database,
                                    String @NotNull [] childTokens,
                                    TokenCompileStream fatherSteam,
                                    CmdCtx<?> fatherCmdCtx) {
        super(database, childTokens, fatherSteam);
        Objects.requireNonNull(fatherCmdCtx);
        this.fatherCmdCtx = fatherCmdCtx;
    }

    @Override
    public ConsumerCtx<?> compile() {
        var cmdCtx = ConsumerKeyword.createCtx(token(), fatherCmdCtx);
        if (cmdCtx == null) {
            throw new CommandCompileException("unknown token: '" + token() + "'");
        }
        next();
        cmdCtx.doAfterCompile(this);
        return cmdCtx;
    }
}
