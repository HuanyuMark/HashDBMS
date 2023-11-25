package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.DataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Date: 2023/11/25 2:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LPushCtx extends PushCtx {

    protected LPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected Integer doPushing(@NotNull List<Object> opsTarget) {
        for (Object value : values.reversed()) {
            Object result;
            // 运行内联命令
            if(value instanceof SupplierCtx supplierCtx) {
                result = supplierCtx.compileResult().get();
            } else {
                result = value;
            }
            opsTarget.addFirst(result);
        }

        return opsTarget.size();
    }
    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LPUSH;
    }
}
