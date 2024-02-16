package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

/**
 * Date: 2023/11/29 0:56
 *
 * @author huanyuMake-pecdle
 */
public class IncCtx extends NumCtx {

    @Override
    public void setStream(SupplierCompileStream stream) {
        super.setStream(stream);
        stream.toWrite();
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.INC;
    }

    @Override
    Number newValue(Number n1, Number n2) {
        if (n1 instanceof Long l1 && n2 instanceof Long l2) {
            return l1 + l2;
        }
        return n1.doubleValue() + n2.doubleValue();
    }
}
