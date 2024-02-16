package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.supplier.*;
import org.hashdb.ms.util.ReflectCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
    MUL(MulCtx.class),
    $$PARAMETER_ACCESS$$(ParameterCtx.class),
    $$VALUE$$(JsonValueCtx.class);

    private static final Map<String, SupplierKeyword> enumsDic;
    private final ReflectCache<? extends SupplierCtx> constructor;

    static {
        SupplierKeyword[] values = values();
        enumsDic = Arrays.stream(values).collect(() -> new HashMap<>(values.length * 5, 0.2F), (m, e) -> m.put(e.name(), e), HashMap::putAll);
    }

    SupplierKeyword(Class<? extends SupplierCtx> keywordCtxClass) {
        this.constructor = new ReflectCache<>(keywordCtxClass);
    }

    public static ReflectCache<? extends SupplierCtx> getCompileCtxConstructor(@NotNull String unknownToken) {
        SupplierKeyword supplierKeyword = typeOfIgnoreCase(unknownToken);
        if (supplierKeyword == null || unknownToken.startsWith("$$") && unknownToken.endsWith("$$")) {
            return null;
        }
        return supplierKeyword.constructor;
    }

    public static SupplierCtx createCtx(@NotNull String unknownToken) {
        var kwCtxConstructor = SupplierKeyword.getCompileCtxConstructor(unknownToken);
        if (kwCtxConstructor == null) {
            return null;
        }
        return kwCtxConstructor.create();
    }


    @Override
    public ReflectCache<? extends SupplierCtx> constructor() {
        return constructor;
    }

    public boolean match(@NotNull String unknownToken) {
        return name().equalsIgnoreCase(unknownToken);
    }

    @Nullable
    public static SupplierKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
        String normalizedStr = unknownToken.toUpperCase();
        return enumsDic.get(normalizedStr);
    }
}
