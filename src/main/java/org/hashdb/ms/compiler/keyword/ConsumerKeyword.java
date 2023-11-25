package org.hashdb.ms.compiler.keyword;

import lombok.SneakyThrows;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LPushCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.RPushCtx;
import org.hashdb.ms.exception.StopComplieException;
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
public enum ConsumerKeyword implements Keyword<ConsumerKeyword> {
    PIPE(PipeCtx.class),
    LPUSH(LPushCtx.class),
    RPUSH(RPushCtx.class);
    private final ConsumerCtxConstructor compileCtxFactory;

    ConsumerKeyword(Class<? extends ConsumerCtx<?>> keywordCtxClass) {
        this.compileCtxFactory = new ConsumerCtxConstructor(keywordCtxClass);
    }

    public static @Nullable ConsumerCtxConstructor getCompileCtxConstructor(@NotNull String unknownToken) {
        ConsumerKeyword consumerKeyword = typeOfIgnoreCase_(unknownToken);
        if(consumerKeyword == null) {
            return null;
        }
        return consumerKeyword.compileCtxFactory;
    }

    @SuppressWarnings("unchecked")
    public static final ConsumerCtxConstructor pipeCmdCtxFactory = PIPE.compileCtxFactory;

    public static ConsumerCtx<?> createCtx(@NotNull String unknownToken, CompileCtx<?> fatherCompileCtx) {
        var kwCtxConstructor = getCompileCtxConstructor(unknownToken);
        if (kwCtxConstructor == null) {
            return null;
        }
        return kwCtxConstructor.create(fatherCompileCtx);
    }

    @Override
    public ConsumerCtxConstructor constructor() {
        return compileCtxFactory;
    }

    public boolean match(@NotNull String unknownToken) {
        var other = getCompileCtxConstructor(unknownToken);
        return compileCtxFactory == other;
    }

    @Override
    @Nullable
    public ConsumerKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
        return typeOfIgnoreCase_(unknownToken);
    }

    public static ConsumerKeyword typeOfIgnoreCase_(@NotNull String unknownToken) {
        String normalizedStr = unknownToken.toUpperCase();
        try {
            ConsumerKeyword keyword = valueOf(normalizedStr);
            if (PIPE == keyword) {
                return null;
            }
            return keyword;
        } catch (IllegalArgumentException e) {
            if (PlaceHolder.pipeline.equals(unknownToken)) {
                return ConsumerKeyword.PIPE;
            }
            return null;
        }
    }

    public static class ConsumerCtxConstructor extends ReflectCacheData<ConsumerCtx<?>> {
        public ConsumerCtxConstructor(Class<? extends ConsumerCtx<?>> keywordCtxClass) {
            super(keywordCtxClass, (clazz) -> {
                try {
                    return clazz.getDeclaredConstructor(CompileCtx.class);
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
        public ConsumerCtx<?> create(CompileCtx<?> fatherCompileCtx) {
            return constructor().newInstance(fatherCompileCtx);
        }
    }

    class PipeCtx extends ConsumerCtx<Object> {

        public PipeCtx(CompileCtx<?> supplierCtx) {
            super(supplierCtx);
            throw new UnsupportedOperationException();
        }
        @Override
        public ConsumerKeyword name() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean checkConsumeType(Object consumeType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<?> supplyType() {
            throw new UnsupportedOperationException();
        }
        @Override
        protected OpsConsumerTask<Object, ?> compile() throws StopComplieException {
            throw new UnsupportedOperationException();
        }
    }
}
