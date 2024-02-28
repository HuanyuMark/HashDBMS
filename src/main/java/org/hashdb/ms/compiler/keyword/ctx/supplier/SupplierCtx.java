package org.hashdb.ms.compiler.keyword.ctx.supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.StopComplieException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:37
 *
 * @author Huanyu Mark
 */
public abstract class SupplierCtx extends CompileCtx<SupplierCompileStream> {

    @JsonIgnore
    private OpsTask<?> compileResult;

    /**
     * 这个由用户通过DataType Symbol指定.
     * 在运行内联命令, 获取执行结果后, 如果用户需要将该值存入数据库中
     * 就需要通过这个DataType来指定要存储成什么类型
     * 如果没有指定, 则通过 {@link DataType#typeofHValue(HValue)} 或
     * {@link DataType#typeOfRawValue(Object)} 判断存储类型
     * 然后克隆一份后再存入数据库
     */
    @Nullable
    private DataType storeType;

    public OpsTask<?> compileWithStream(SupplierCompileStream compileStream) throws StopComplieException {
        if (compileResult != null) {
            throw new DBSystemException(getClass().getSimpleName() + " is finish compilation");
        }
        setStream(compileStream);
        // 必须要先在当前线程中编译, 提前发现编译错误
        Supplier<?> supplierTask = compile();
        // 支持管道操作, 将原 生产型任务生产的 结果传给下一个消费者任务使用
        this.compileResult = OpsTask.of(!compileStream.isWrite(), () -> callConsumer(supplierTask.get()));
        return this.compileResult;
    }

    public OpsTask<?> executeWithStream(SupplierCompileStream stream) {
        setStream(stream);
        this.compileResult = OpsTask.of(() -> callConsumer(executor().get()));
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
    public @NotNull Class<?> supplyType() {
        return Object.class;
    }

    @NotNull
    public DataType storeType() {
        if (storeType != null) {
            return storeType;
        }
        Set<DataType> supportedDataType = DataType.typeOfCloneable(supplyType());
        if (supportedDataType == null) {
            throw new DBSystemException("class '" + getClass() + "' supplyType is '" + supplyType() + "', can not found cloneable DataType");
        }
        for (DataType dataType : supportedDataType) {
            storeType = dataType;
            return dataType;
        }
        throw new DBSystemException("supportedDataType is illegal");
    }

    public void setStoreType(@Nullable DataType storeType) {
        this.storeType = storeType;
    }

    @Override
    abstract public SupplierKeyword name();
}
