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
 */
public class SetCtx extends WriteSupplierCtx {

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.SET;
    }

    @Override
    protected @Nullable HValue<?> doMutation(String key, Object rawValue, Long expireMillis, OpsTaskPriority priority) {
        // 如果这个key被原始字符串标记 R()
        String parameterName = extractOriginalString(key);
        if (parameterName == null) {
            return stream().db().set(key, rawValue, expireMillis, priority);
        }
        // 这个parameterName必须要在编译时校验, 也就是说检查是否是$开头
        stream().session().setParameter(parameterName, rawValue);
        return null;
    }
}
