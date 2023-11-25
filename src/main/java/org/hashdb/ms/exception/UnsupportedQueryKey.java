package org.hashdb.ms.exception;

import lombok.experimental.StandardException;
import org.hashdb.ms.util.JacksonSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Date: 2023/11/24 23:12
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@StandardException
public class UnsupportedQueryKey extends DBExternalException {
    @Contract("_ -> new")
    public static @NotNull UnsupportedQueryKey of(List<?> unsupportedKeys) {
        return new UnsupportedQueryKey("can not query key of these value: " + JacksonSerializer.stringfy(unsupportedKeys));
    }
}
