package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.PopOpCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;

import java.util.List;
import java.util.stream.Stream;

/**
 * Date: 2023/11/30 0:18
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class PopPushCtx extends MutableListCtx {

    protected Integer popCount = 1;

    protected List<Object> valueOrSuppliers;

    protected PopPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        var pops = doPop(opsTarget);
        Stream<Object> values = valueOrSuppliers.parallelStream().map(valueOrSupplier -> {
            if (valueOrSupplier instanceof SupplierCtx valueSupplier) {
                return selectOne(getSuppliedValue(valueSupplier));
            }
            return valueOrSupplier;
        });
        doPush(opsTarget, values);
        return pops;
    }

    abstract protected List<Object> doPop(List<Object> opsTarget);

    abstract protected void doPush(List<Object> opsTarget, Stream<Object> values);

    @Override
    void beforeCompile() {
        doCompile();
        beforeCompilePipe();
    }

    protected void doCompile() {
        while (true) {
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();

                compileJsonValues((dataType, valueOrSupplier) -> {
                    valueOrSuppliers.add(valueOrSupplier);
                    return true;
                });

                if (compileOptions(op -> {
                    if (op instanceof PopOpCtx popOp) {
                        popCount = popOp.value();
                    }
                    addOption(op);
                    return true;
                })) {
                    return;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (valueOrSuppliers.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease 1 value to push");
        }
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
    }
}
