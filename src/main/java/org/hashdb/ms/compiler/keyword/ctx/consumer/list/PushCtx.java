package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.StopComplieException;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/26 19:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class PushCtx extends MutateListCtx {
    protected final List<Object> values = new LinkedList<>();

    protected PushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected OpsConsumerTask<List<?>, ?> compile() throws StopComplieException {
        compileJsonValues(values::add);
        return (returnList) -> {
            @SuppressWarnings("unchecked")
            List<Object> opsTarget = ((List<Object>) returnList.getFirst());
            return OpsTask.of(()-> doPushing(opsTarget));
        };
    }

    abstract protected Integer doPushing(List<Object> opsTarget);

    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }
}
