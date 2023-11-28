package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.Keyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:37
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class SupplierCtx extends CompileCtx<SupplierCompileStream> {

    private OpsTask<?> compileResult;

    public OpsTask<?> compileWithStream(SupplierCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBInnerException(getClass().getSimpleName() + " is finish compilation");
        }
        stream = compileStream;
        var supplierTask = compile();
        // 支持管道操作, 将原 生产型任务生产的 结果传给下一个消费者任务使用
        this.compileResult = OpsTask.of(()-> consumeWithConsumer(supplierTask.get()));
        return this.compileResult;
    }

    abstract protected Supplier<?> compile() throws StopComplieException;

    protected SupplierCtx(Map<Class<? extends OptionCtx<?>>, OptionCtx<?>> initialOptions) {
        super(initialOptions);
    }

    protected SupplierCtx() {
        super(new HashMap<>());
    }

    public OpsTask<?> compileResult() {
        return compileResult;
    }

    @Override
    public Class<?> supplyType() {
        return Object.class;
    }

    @Override
    abstract public SupplierKeyword name();
}
