package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

/**
 * Date: 2023/11/29 0:56
 *
 * @author huanyuMake-pecdle
 */
public class MulCtx extends NumCtx {

    @Override
    public void setStream(SupplierCompileStream stream) {
        super.setStream(stream);
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
