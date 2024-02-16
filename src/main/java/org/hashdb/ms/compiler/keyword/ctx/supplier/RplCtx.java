package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.OpsTaskPriority;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 */
public class RplCtx extends WriteSupplierCtx {


    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.RPL;
    }

    @Override
    protected @Nullable HValue<?> doMutation(String key, Object rawValue, Long expireMillis, OpsTaskPriority priority) {
        // 如果这个key被原始字符串标记 R()
        String parameterName = extractOriginalString(key);
        if (parameterName == null) {
            return stream().db().rpl(key, rawValue, expireMillis, priority);
        }
        // 这个parameterName必须要在编译时校验, 也就是说检查是否是$开头
        stream().session().setParameter(parameterName, rawValue);
        return null;
    }
}
