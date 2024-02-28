package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;

import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author Huanyu Mark
 */
public class FlushCtx extends SupplierCtx {

    @Override
    public void setStream(SupplierCompileStream stream) {
        super.setStream(stream);
        stream.toWrite();
    }

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.FLUSH;
    }

    @Override
    public Supplier<?> compile() {
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            stream().db().clear();
            return true;
        };
    }
}
