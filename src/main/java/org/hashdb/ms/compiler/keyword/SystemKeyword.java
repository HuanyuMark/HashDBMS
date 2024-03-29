package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.sys.*;
import org.hashdb.ms.util.ReflectCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/30 0:47
 *
 * @author Huanyu Mark
 */
public enum SystemKeyword implements Keyword<SystemKeyword> {
    DBUSE(DBUseCtx.class),
    DBCREATE(DBCreateCtx.class),
    DBSHOW(DBShowCtx.class),
    DBCURRENT(DBCurrentCtx.class);

    private final ReflectCache<? extends SystemCompileCtx<?>> constructor;

    SystemKeyword(Class<? extends SystemCompileCtx<?>> clazz) {
        this.constructor = new ReflectCache<>(clazz);
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
    public ReflectCache<? extends SystemCompileCtx<?>> constructor() {
        return constructor;
    }
}
