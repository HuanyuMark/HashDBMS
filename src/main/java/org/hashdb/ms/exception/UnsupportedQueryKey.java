package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Date: 2023/11/24 23:12
 *
 * @author Huanyu Mark
 */
@StandardException
public class UnsupportedQueryKey extends DBClientException {

    @Contract("_ -> new")
    public static @NotNull UnsupportedQueryKey of(List<?> unsupportedKeys) {
        return of(unsupportedKeys, null);
    }

    public static @NotNull UnsupportedQueryKey of(List<?> unsupportedKeys, String errorMsg) {
        return new UnsupportedQueryKey("can not query key of these value: " + JsonService.toString(unsupportedKeys) + "." + (errorMsg == null ? "" : errorMsg));
    }

    public static UnsupportedQueryKey of(Enum<?> keyword, SupplierCtx supplierCtx) {
        return new UnsupportedQueryKey("keyword '" + keyword.name() + "' require string return type to query, but " +
                "receive a illegal type from supplier command '" + supplierCtx.command() + "'");
    }
}
