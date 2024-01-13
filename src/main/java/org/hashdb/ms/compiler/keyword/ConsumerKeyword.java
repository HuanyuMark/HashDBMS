package org.hashdb.ms.compiler.keyword;

import lombok.SneakyThrows;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.DelCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.GetCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.SetCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.*;
import org.hashdb.ms.compiler.keyword.ctx.consumer.set.SSetCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

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
    RPUSH(RPushCtx.class),
    LPOP(LPopCtx.class),
    RPOP(RPopCtx.class),
    /**
     * 这些关键字在很多其它数据结构都会用到, 具有一定的模糊性
     * 会导致编译器在编译时, 不可避免地转入解释模式(即使已经采用了预编译的手段尽力加快解释速度),
     * 命令会在运行时, 根据管道从上一级命令中的返回值, 动态确认使用-
     * 哪个关键字的解释器, 因为使用了解释模式, 且HashDB操作一个数据库,
     * 使用的库级别的任务队列生产消费者模式进行操作
     * 导致消费者线程在执行命令时, 要边执行边编译, 降低整个数据库
     * 的数据吞吐量, 非常影响性能
     * 所以, 建议使用各个数据类型提供的特化命令, 确保编译器完全使用编译模式
     *
     * @see #DEL
     * @see #SET
     * @see #GET
     */
    DEL(DelCtx.class),
    SET(SetCtx.class),
    GET(GetCtx.class),
    LEN(SizeCtx.class),
    SIZE(SizeCtx.class),
    // 链表特化指令
    LDEL(LDelCtx.class),
    LSET(LSetCtx.class),
    LGET(LGetGtx.class),
    REVERSE(LReverseCtx.class),
    TRIM(TrimCtx.class),
    LPOPRPUSH(LPopRPush.class),
    RPOPLPUSH(RPopLPush.class),
    SSET(SSetCtx.class);
    private final ConsumerCtxConstructor compileCtxFactory;

    ConsumerKeyword(Class<? extends ConsumerCtx<?>> keywordCtxClass) {
        this.compileCtxFactory = new ConsumerCtxConstructor(keywordCtxClass);
    }

    public static @Nullable ConsumerCtxConstructor getCompileCtxConstructor(@NotNull String unknownToken) {
        ConsumerKeyword consumerKeyword = typeOfIgnoreCase(unknownToken);
        if (consumerKeyword == null) {
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

    @Nullable
    public static ConsumerKeyword typeOfIgnoreCase(@NotNull String unknownToken) {
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

    static class PipeCtx extends ConsumerCtx<Object> {

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
        public @NotNull Class<?> supplyType() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Function<Object, ?> compile() throws StopComplieException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Function<Object, ?> executor() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected DataType consumableHValueType() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Class<?> consumableModifiableClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Class<?> consumableUnmodifiableClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Object operateWithMutableList(Object opsTarget) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Object operateWithImmutableList(Object opsTarget) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Object operateWithHValue(HValue<Object> opsTarget) {
            throw new UnsupportedOperationException();
        }
    }
}
