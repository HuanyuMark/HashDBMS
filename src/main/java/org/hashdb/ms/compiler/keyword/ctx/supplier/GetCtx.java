package org.hashdb.ms.compiler.keyword.ctx.supplier;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.data.HValue;

import java.util.List;

/**
 * Date: 2023/11/24 16:20
 * 等效于命令：
 * GET $KEY … $KEY
 * GET LIKE $PATTERN1 [,$LIMIT]
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class GetCtx extends ReadSupplierCtx {

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.GET;
    }

    @Override
    protected List<?> doQueryLike(String pattern) {
        var limitOption = getOption(LimitOpCtx.class);
        return stream.db().getLike(pattern, limitOption == null ? null : limitOption.value());
    }

    @Override
    protected Object doQuery(String key) {
        return HValue.unwrapData(stream.db().get(CompileCtx.normalizeToQueryKey(key)));
    }
}
