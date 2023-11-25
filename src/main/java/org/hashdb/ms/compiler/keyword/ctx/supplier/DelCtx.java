package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.data.OpsTask;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DelCtx extends SupplierCtx {
    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.DEL;
    }
    @Override
    public OpsTask<?> compile(SupplierCompileStream compileStream) {
    return null;
    }
}
