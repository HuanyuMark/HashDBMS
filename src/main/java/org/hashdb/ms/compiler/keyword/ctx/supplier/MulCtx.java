package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;

/**
 * Date: 2023/11/29 0:56
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class MulCtx extends NumCtx {
    {
        stream.toWrite();
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.MUL;
    }

    @Override
    Number newValue(Number n1, Number n2) {
        return n1.doubleValue() * n2.doubleValue();
    }
}
