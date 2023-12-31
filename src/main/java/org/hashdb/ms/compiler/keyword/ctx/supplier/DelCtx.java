package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.data.HValue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DelCtx extends ReadSupplierCtx {
    {
        stream.toWrite();
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.DEL;
    }

    @Override
    protected List<HValue<?>> doQueryLike(Pattern pattern) {
        LimitOpCtx limitOpCtx = getOption(LimitOpCtx.class);
        return stream.db().delLike(pattern, limitOpCtx == null ? null : limitOpCtx.value());
    }

    @Override
    protected HValue<?> doQuery(String key) {
        return stream.db().del(key);
    }
}
