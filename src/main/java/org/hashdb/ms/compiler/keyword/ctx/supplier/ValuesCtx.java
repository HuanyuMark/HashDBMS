package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.task.ImmutableChecker;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ValuesCtx extends SupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.VALUES;
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        LimitOpCtx limitOpCtx = getOption(LimitOpCtx.class);
        return () -> {
            Stream<Object> stream = this.stream.db().values().stream().map(HValue::data);
            if (limitOpCtx != null) {
                return stream.limit(limitOpCtx.value()).toList();
            }
            return stream.toList();
        };
    }

    private void doCompile() {
        while (true) {
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllKeywords();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            if (compileOptions(op -> {
                addOption(op);
                return true;
            })) {
                return;
            }
            stream.next();
        }
    }
}
