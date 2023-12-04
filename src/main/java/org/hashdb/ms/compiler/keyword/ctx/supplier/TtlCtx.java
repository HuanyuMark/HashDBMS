package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.KeywordModifier;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/24 16:20
 * 还有多少毫秒过期
 * TTL $KEY ... $KEY
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class TtlCtx extends ReadSupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.TTL;
    }

    @Override
    protected Object doQuery(String key) {
        return stream.db().ttl(key);
    }

    @Override
    protected void beforeCompileModifier(KeywordModifier modifier) {
        throw new CommandCompileException("keyword '" + name() + "' can not use modifier '" + modifier + "'");
    }
}
