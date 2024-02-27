package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.PopOpCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

/**
 * Date: 2023/11/30 0:18
 *
 * @author huanyuMake-pecdle
 */
public abstract class PopPushCtx extends MutableListCtx {
    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
        stream.toWrite();
    }

    protected Integer popCount = 1;

    protected List<Object> valueOrSuppliers;

    protected PopPushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        var pops = doPop(opsTarget);
        Stream<Object> values = valueOrSuppliers.parallelStream().map(valueOrSupplier -> {
            if (valueOrSupplier instanceof SupplierCtx valueSupplier) {
                return selectOneValue(exeSupplierCtx(valueSupplier));
            }
            return valueOrSupplier;
        });
        doPush(opsTarget, values);
        return pops;
    }

    abstract protected List<Object> doPop(List<Object> opsTarget);

    abstract protected void doPush(List<Object> opsTarget, Stream<Object> values);

    @Override
    protected void beforeCompile() {
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
