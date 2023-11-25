package org.hashdb.ms.compiler.keyword;

import lombok.SneakyThrows;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.PipeCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LPushCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.RPushCtx;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Date: 2023/11/25 0:49
 * 可管道化关键字, 需要入参才能执行
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum ConsumerKeyword implements Keyword {
    PIPE(PipeCtx.class),
    LPUSH(LPushCtx.class),
    RPUSH(RPushCtx.class);
    private final ConsumerCtxConstructor cmdCtxFactory;

    ConsumerKeyword(Class<? extends ConsumerCtx<?>> keywordCtxClass) {
        this.cmdCtxFactory = new ConsumerCtxConstructor(keywordCtxClass);
    }

    public static @Nullable ConsumerCtxConstructor getCmdCtxConstructor(@NotNull String unknownToken) {
        String normalizedStr = unknownToken.toUpperCase();
        try {
            ConsumerKeyword keyword = valueOf(normalizedStr);
            if (PIPE == keyword) {
                return null;
            }
            return keyword.cmdCtxFactory;
        } catch (IllegalArgumentException e) {
            if ("|".equals(unknownToken)) {
                return pipeCmdCtxFactory;
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static final ConsumerCtxConstructor pipeCmdCtxFactory = PIPE.cmdCtxFactory;

    public static ConsumerCtx<?> createCtx(@NotNull String unknownToken, CmdCtx<?> fatherCmdCtx) {
        var kwCtxConstructor = getCmdCtxConstructor(unknownToken);
        if (kwCtxConstructor == null) {
            return null;
        }
        return kwCtxConstructor.create(fatherCmdCtx);
    }

    @Override
    public ConsumerCtxConstructor cmdCtxFactory() {
        return cmdCtxFactory;
    }

    public boolean match(@NotNull String unknownToken) {
        var other = getCmdCtxConstructor(unknownToken);
        return cmdCtxFactory == other;
    }

    public static class ConsumerCtxConstructor extends ReflectCacheData<ConsumerCtx<?>> {
        public ConsumerCtxConstructor(Class<? extends ConsumerCtx<?>> keywordCtxClass) {
            super(keywordCtxClass, (clazz) -> {
                try {
                    return clazz.getDeclaredConstructor(CmdCtx.class);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public ConsumerCtx<?> create() {
            throw new UnsupportedOperationException();
        }

        @SneakyThrows
        public ConsumerCtx<?> create(CmdCtx<?> fatherCmdCtx) {
            return constructor().newInstance(fatherCmdCtx);
        }
    }
}
