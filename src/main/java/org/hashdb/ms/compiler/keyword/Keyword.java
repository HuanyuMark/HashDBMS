package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/25 3:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface Keyword<E extends Enum<E>> {

    String name();

    boolean match(@NotNull String unknownToken);

    @Nullable
    E typeOfIgnoreCase(@NotNull String unknownToken);

    ReflectCacheData<? extends CompileCtx<?>> constructor();
}
