package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;

import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CountCtx extends SupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.COUNT;
    }

    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }

    @Override
    public Supplier<?> compile() {
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> stream.db().count();
    }
}
