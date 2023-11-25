package org.hashdb.ms.compiler.keyword.ctx.consumer;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.StopComplieException;

/**
 * Date: 2023/11/25 13:28
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class ConsumerCtx<I> extends CmdCtx<ConsumerCompileStream> {

    protected final CmdCtx<?> fatherCmdCtx;

    protected OpsConsumerTask<I, ?> compileResult;

    protected ConsumerCtx(CmdCtx<?> fatherCmdCtx) {
        this.fatherCmdCtx = fatherCmdCtx;
    }

    @Override
    abstract public ConsumerKeyword name();

    abstract protected boolean checkConsumeType(Class<?> supplierClass);

    public OpsConsumerTask<I, ?> doAfterCompile(ConsumerCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBInnerException(getClass().getSimpleName() + " is finish compilation");
        }
        this.stream = compileStream;
        compileResult = compile();
        return compileResult;
    }

    abstract protected OpsConsumerTask<I, ?> compile() throws StopComplieException;

    public OpsTask<?> compileResult(I opsTargetSupplier) {
        return compileResult.apply(opsTargetSupplier);
    }
}
