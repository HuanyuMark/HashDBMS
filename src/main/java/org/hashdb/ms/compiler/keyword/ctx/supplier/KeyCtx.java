package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;

import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 */
public class KeyCtx extends SupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.KEY;
    }

    @Override
    public Supplier<?> compile() {
        return null;
    }

    @Override
    public Supplier<?> executor() {
        return null;
    }
}
