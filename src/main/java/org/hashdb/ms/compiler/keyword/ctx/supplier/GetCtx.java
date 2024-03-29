package org.hashdb.ms.compiler.keyword.ctx.supplier;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.data.HValue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Date: 2023/11/24 16:20
 * 等效于命令：
 * GET $KEY … $KEY
 * GET LIKE $PATTERN1 [,$LIMIT]
 *
 * @author Huanyu Mark
 */
@Slf4j
public class GetCtx extends ReadSupplierCtx {

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.GET;
    }

    @Override
    protected List<HValue<?>> doQueryLike(Pattern pattern) {
        var limitOption = getOption(LimitOpCtx.class);
        return stream().db().getLike(pattern, limitOption == null ? null : limitOption.value());
    }

    @Override
    protected HValue<?> doQuery(String key) {
        return stream().db().get(key);
    }
}
