package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.exception.StopComplieException;

import java.util.List;

/**
 * Date: 2023/11/25 2:45
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class RPushCtx extends PushCtx {

    protected RPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.RPUSH;
    }

    @Override
    protected Integer doPushing(List<Object> opsTarget) {
        for (Object value : values) {
            Object result;
            if(value instanceof SupplierCtx supplierCtx) {
                result = supplierCtx.compileResult().get();
            } else {
                result = value;
            }
            opsTarget.add(result);
        }
        return opsTarget.size();
    }
}
