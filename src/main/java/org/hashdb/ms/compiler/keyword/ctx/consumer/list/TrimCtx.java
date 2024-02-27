package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.DeleteOpCtx;
import org.hashdb.ms.compiler.option.HDeleteOpCtx;
import org.hashdb.ms.compiler.option.LDeleteOpCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/29 9:57
 *
 * @author huanyuMake-pecdle
 */
public class TrimCtx extends MutableListCtx {
    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
        stream().toWrite();
    }

    protected boolean delete = false;

    protected OpsTaskPriority deletePriority;

    protected Object limitOrSupplier = 1;

    protected TrimCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.TRIM;
    }

    @Override
    protected void beforeCompile() {
        doCompile();
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        long popCount;
        long limit;
        if (limitOrSupplier instanceof SupplierCtx limitSupplier) {
            limitOrSupplier = (exeSupplierCtx(limitSupplier));
        }
        limit = (Long) selectOneKeyOrElseThrow(limitOrSupplier);

        if (limit << 1 < opsTarget.size()) {
            popCount = limit;
        } else {
            if (opsTarget.size() % 2 == 0) {
                popCount = opsTarget.size() >> 1;
            } else {
                popCount = (opsTarget.size() - 1) >> 1;
            }
        }
        List<Object> left = new LinkedList<>();
        List<Object> right = new LinkedList<>();
        for (int i = 0; i < popCount; i++) {
            left.add(opsTarget.removeFirst());
            right.add(opsTarget.removeLast());
        }
        left.addAll(right);
        return Collections.unmodifiableList(left);
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsTarget) {
        return operateWithMutableList(opsTarget.data());
    }

    private void doCompile() {
        String token;
        try {
            if (compilePipe()) {
                return;
            }
            filterAllKeywords();
            token = stream().token();

            limitOrSupplier = compileInlineCommand();
            if (limitOrSupplier == null) {
                try {
                    limitOrSupplier = Long.valueOf(token);
                    if (((Long) limitOrSupplier) < 0) {
                        throw new CommandCompileException("keyword '" + name() + "' require a positive number as $COUNT limit");
                    }
                    stream().next();
                } catch (NumberFormatException e) {
                    throw new CommandCompileException("can not parse string '" + token + "' to number");
                }
            }

            compileOptions(op -> {
                if (op instanceof DeleteOpCtx deleteOpCtx) {
                    delete = true;
                } else if (op instanceof HDeleteOpCtx deleteOpCtx) {
                    delete = true;
                    deletePriority = OpsTaskPriority.HIGH;
                } else if (op instanceof LDeleteOpCtx deleteOpCtx) {
                    delete = true;
                    deletePriority = OpsTaskPriority.LOW;
                }
                addOption(op);
                return true;
            });
        } catch (ArrayIndexOutOfBoundsException ignore) {
        }
    }
}
