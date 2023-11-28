package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.compiler.option.LongOpCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.task.ImmutableChecker;

import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 * 等效于命令:
 * KEYS $LIMIT
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class KeysCtx extends SupplierCtx {

    private Long limit;
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.KEYS;
    }

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableCollection;
    }

    @Override
    public Supplier<?> compile() {
        doCompile();
        return ()->{
            if(limit == null) {
                return stream.db().keys();
            }
            return stream.db().keys().stream().limit(limit).toList();
        };
    }

    private void doCompile() {
        while (true) {
            if (compileOptions(op->{
                addOption(op);
                return true;
            })) {
                break;
            }
        }
        LimitOpCtx limitOpCtx = getOption(LimitOpCtx.class);
        if(limitOpCtx == null) {
            return;
        }
        limit = limitOpCtx.value();
    }
}
