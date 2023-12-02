package org.hashdb.ms.compiler.keyword.ctx.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Date: 2023/11/25 13:28
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class ConsumerCtx<I> extends CompileCtx<ConsumerCompileStream> {

    protected final CompileCtx<?> fatherCompileCtx;

    protected Function<I, ?> compileResult;

    protected ConsumerCtx(CompileCtx<?> fatherCompileCtx) {
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    abstract public ConsumerKeyword name();

    abstract protected boolean checkConsumeType(Object consumeType) throws CommandExecuteException;

    public Function<I, ?> compileWithStream(ConsumerCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBSystemException(getClass().getSimpleName() + " is finish compilation");
        }
        this.stream = compileStream;
        compileResult = opsTarget -> consumeWithConsumer(compile().apply(opsTarget));
        return compileResult;
    }

    abstract protected Function<I, ?> compile() throws StopComplieException;

    public Object consume(I opsTarget) {
        if (opsTarget == null) {
            throw new CommandExecuteException("keyword '" + name() + "' can not consume type: 'null' return from '"+stream.fatherCommand()+"'." + stream.errToken(stream.token()));
        }
        if (!checkConsumeType(opsTarget)) {
            throw new CommandExecuteException("keyword '" + name() + "' can not consume type: '" + DataType.typeOfRawValue(opsTarget) + "' return from '"+stream.fatherCommand()+"'." + stream.errToken(stream.token()));
        }
        return compileResult.apply(opsTarget);
    }

    protected HValue<List<?>> extractHValue(@NotNull List<?> returnList) throws CommandExecuteException {
        if (!(returnList.getFirst() instanceof HValue<?> opsValue)) {
            throw new CommandExecuteException("keyword '" + name() + "' con not consume return value of supplier command '" + fatherCompileCtx.command() + "'");
        }
        DataType returnType = DataType.typeofHValue(opsValue);
        if (returnType != DataType.LIST) {
            throw new CommandExecuteException("keyword '" + name() + "' con not consume return type '" + returnType + "' of supplier command '" + fatherCompileCtx.command() + "'");
        }
        return (HValue<List<?>>) opsValue;
    }
}
