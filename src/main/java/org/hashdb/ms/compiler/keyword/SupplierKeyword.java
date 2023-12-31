package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.supplier.*;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/24 16:22
 * 提供者关键字的特点就是, 构造的任务没有入参
 * 所以, 一般就用来当作命令的开头关键字
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum SupplierKeyword implements Keyword<SupplierKeyword> {
    GET(GetCtx.class),
    SET(SetCtx.class),
    RPL(RplCtx.class),
    KEY(KeyCtx.class),
    KEYS(KeysCtx.class),
    COUNT(CountCtx.class),
    VALUES(ValuesCtx.class),
    EXISTS(ExistsCtx.class),
    DEL(DelCtx.class),
    CLEAR(ClearCtx.class),
    TTL(TtlCtx.class),
    FLUSH(FlushCtx.class),
    TYPE(TypeCtx.class),
    EXPIRE(ExpireCtx.class),
    INC(IncCtx.class),
    MUL(MulCtx.class);

    private final ReflectCacheData<? extends SupplierCtx> constructor;

    SupplierKeyword(Class<? extends SupplierCtx> keywordCtxClass) {
        this.constructor = new ReflectCacheData<>(keywordCtxClass);
    }

    public static ReflectCacheData<? extends SupplierCtx> getCompileCtxConstructor(@NotNull String unknownToken) {
        SupplierKeyword supplierKeyword = typeOfIgnoreCase(unknownToken);
        if (supplierKeyword == null) {
            return null;
        }
        return supplierKeyword.constructor;
    }

    public static boolean is(@NotNull String keyword) {
        try {
            valueOf(keyword.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static SupplierCtx createCtx(@NotNull String unknownToken) {
        var kwCtxConstructor = SupplierKeyword.getCompileCtxConstructor(unknownToken);
        if (kwCtxConstructor == null) {
            return null;
        }
        return kwCtxConstructor.create();
    }


    @Override
    public ReflectCacheData<? extends SupplierCtx> constructor() {
        return constructor;
    }

    public boolean match(@NotNull String unknownToken) {
        return name().equalsIgnoreCase(unknownToken);
    }

    @Nullable
    public static SupplierKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
        String normalizedStr = unknownToken.toUpperCase();
        try {
            return valueOf(normalizedStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
