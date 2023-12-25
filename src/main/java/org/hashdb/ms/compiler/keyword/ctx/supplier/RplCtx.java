package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class RplCtx extends WriteSupplierCtx {


    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.RPL;
    }

    @Override
    protected @Nullable HValue<?> doMutation(String key, Object value, Long expireMillis, OpsTaskPriority priority) {
        return stream().db().rpl(key, value, expireMillis, priority);
    }
}
