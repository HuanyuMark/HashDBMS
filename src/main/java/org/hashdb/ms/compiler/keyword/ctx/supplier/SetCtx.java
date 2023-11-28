package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/24 16:20
 * 等效于命令：
 * SET $KEY [#TYPE_SYMBOL,]$VALUE[,--[h]expire=$MILLIS] ... $KEY [#TYPE_SYMBOL,]$VALUE[,--[h]expire=$MILLIS]
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class SetCtx extends WriteSupplierCtx {

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.SET;
    }

    @Override
    protected @Nullable HValue<?> doMutation(String key, Object value, Long expireMillis, OpsTaskPriority priority) {
        return stream.db().set(key, value, expireMillis, priority);
    }
}
