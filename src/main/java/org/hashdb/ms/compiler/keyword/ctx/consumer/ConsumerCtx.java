package org.hashdb.ms.compiler.keyword.ctx.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.function.Supplier;

/**
 * Date: 2023/11/25 13:28
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class ConsumerCtx<I> extends CompileCtx<ConsumerCompileStream> {

    protected final CompileCtx<?> fatherCompileCtx;

    protected OpsConsumerTask<I, ?> compileResult;

    protected ConsumerCtx(CompileCtx<?> fatherCompileCtx) {
        this.fatherCompileCtx = fatherCompileCtx;
    }

    @Override
    abstract public ConsumerKeyword name();

    abstract protected boolean checkConsumeType(Object consumeType);

    public OpsConsumerTask<I, ?> compileWithStream(ConsumerCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBInnerException(getClass().getSimpleName() + " is finish compilation");
        }
        this.stream = compileStream;
        compileResult = compile();

        return opsTarget -> ()->consumeWithConsumer(compileResult.apply(opsTarget).get());
    }

    abstract protected OpsConsumerTask<I, ?> compile() throws StopComplieException;

    public Supplier<?> compileResult(I opsTarget) {
        if(opsTarget == null) {
            throw new CommandExecuteException("keyword '"+name()+"' can not consume type: 'null'."+stream.errToken(stream.token()));
        }
        if(!checkConsumeType(opsTarget)) {
            throw new CommandExecuteException("keyword '"+name()+"' can not consume type: '"+ DataType.typeOfRawValue(opsTarget) +"'");
        }
        return compileResult.apply(opsTarget);
    }
}
