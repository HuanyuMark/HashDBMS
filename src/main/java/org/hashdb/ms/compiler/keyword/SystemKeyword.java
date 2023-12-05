package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.sys.DBCreateCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBShowCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.DBUseCtx;
import org.hashdb.ms.compiler.keyword.ctx.sys.SystemCompileCtx;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 0:47
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum SystemKeyword implements Keyword<SystemKeyword> {
    DBUSE(DBUseCtx.class),
    DBCREATE(DBCreateCtx.class),
    DBSHOW(DBShowCtx.class);

    private final ReflectCacheData<? extends SystemCompileCtx<?>> constructor;

    SystemKeyword(Class<? extends SystemCompileCtx<?>> clazz) {
        this.constructor = new ReflectCacheData<>(clazz);
    }

    public static @Nullable SystemCompileCtx<?> createCtx(String unknownToken) {
        SystemKeyword systemKeyword = typeOfIgnoreCase(unknownToken);
        if (systemKeyword == null) {
            return null;
        }
        return systemKeyword.constructor.create();
    }

    @Override
    public boolean match(@NotNull String unknownToken) {
        return name().equalsIgnoreCase(unknownToken);
    }

    @Nullable
    public static SystemKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
        String normalizedStr = unknownToken.toUpperCase();
        try {
            return valueOf(normalizedStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public ReflectCacheData<? extends SystemCompileCtx<?>> constructor() {
        return constructor;
    }
}
