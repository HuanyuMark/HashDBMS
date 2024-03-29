package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author Huanyu Mark
 */
public class CountCtx extends SupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.COUNT;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return Integer.class;
    }

    @Override
    public Supplier<?> compile() {
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> stream().db().count();
    }
}
