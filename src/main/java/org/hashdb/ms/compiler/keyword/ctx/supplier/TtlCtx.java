package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.KeyStringModifier;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.DBInnerException;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 * 还有多少毫秒过期
 * TTL $KEY ... $KEY
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class TtlCtx extends ReadSupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.TTL;
    }

    @Override
    protected List<?> doQueryLike(String pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object doQuery(String key) {
        return stream.db().ttl(key);
    }

    @Override
    protected void beforeCompileModifier(KeyStringModifier modifier) {
        throw new CommandCompileException("keyword '"+name()+"' can not use modifier '"+modifier+"'");
    }
}
