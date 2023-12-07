package org.hashdb.ms.compiler.keyword.ctx.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBSystemException;
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

    @JsonIgnore
    private OpsTask<?> compileResult;

    public OpsTask<?> compileWithStream(SupplierCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBSystemException(getClass().getSimpleName() + " is finish compilation");
        }
        stream = compileStream;
        // 支持管道操作, 将原 生产型任务生产的 结果传给下一个消费者任务使用
        this.compileResult = OpsTask.of(() -> consumeWithConsumer(compile().get()));
        return this.compileResult;
    }

    public OpsTask<?> executeWithStream(SupplierCompileStream stream) {
        this.stream = stream;
        this.compileResult = OpsTask.of(() -> consumeWithConsumer(executor().get()));
        return this.compileResult;
    }

    /**
     * 编译命令, 形成当前Ctx,然后生成使用当前Ctx的执行器
     */
    abstract protected Supplier<?> compile() throws StopComplieException;

    /**
     * 生成使用当前Ctx的执行器, 但不编译
     */
    public abstract Supplier<?> executor();

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
