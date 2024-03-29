package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.keyword.KeywordModifier;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

/**
 * Date: 2023/11/24 16:20
 * 还有多少毫秒过期
 * TTL $KEY ... $KEY
 *
 * @author Huanyu Mark
 */
public class TtlCtx extends ReadSupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.TTL;
    }

    @Override
    protected Object doQuery(String key) {
        return stream().db().ttl(key);
    }

    @Override
    protected void beforeCompileModifier(KeywordModifier modifier) {
        throw new CommandCompileException("keyword '" + name() + "' can not use modifier '" + modifier + "'");
    }
}
