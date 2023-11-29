package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 0:47
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum SystemKeyword implements Keyword<SystemKeyword>{
    USE;

    @Override
    public boolean match(@NotNull String unknownToken) {
        return false;
    }

    @Override
    public @Nullable SystemKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
        return null;
    }

    @Override
    public ReflectCacheData<? extends CompileCtx<?>> constructor() {
        return null;
    }
}
