package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.exception.UnsupportedQueryKey;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExistsCtx extends SupplierCtx {

    private final GetCtx getCtx = new GetCtx();

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.EXISTS;
    }

    @Override
    public Supplier<?> compile() {
        getCtx.compileWithStream(stream());
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            int[] index = {0};
            return getCtx.keyOrSupplier.stream().flatMap(keyOfSupplier -> {
                String key;
                if (keyOfSupplier instanceof SupplierCtx supplierCtx) {
                    try {
                        key = normalizeToQueryKey(supplierCtx);
                    } catch (UnsupportedQueryKey e) {
                        throw UnsupportedQueryKey.of(name(), supplierCtx);
                    }
                } else {
                    key = (String) keyOfSupplier;
                }
                if (stream().db().exists(key)) {
                    return Stream.of(index[0]++);
                }
                return Stream.empty();
            }).toList();
        };
    }
}
