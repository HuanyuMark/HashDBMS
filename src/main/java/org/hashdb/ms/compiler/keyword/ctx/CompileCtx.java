package org.hashdb.ms.compiler.keyword.ctx;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.Keyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.util.JsonService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Date: 2023/11/25 0:34
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class CompileCtx<S extends DatabaseCompileStream> implements CompilerNode {
    protected Map<Class<? extends OptionCtx<?>>, OptionCtx<?>> options;


    protected S stream;
    /**
     * 是否有管道符? 如果有, 则会有管道Ctx, 其会包装其它的 ConsumerCtx
     * 在当前 SupplierCtx的任务执行完后, 会接收这个Supplier 产生的结果
     * 当作ConsumerCtx的操作对象, 进行消费
     */
    protected ConsumerCtx<Object> consumerCtx;

    protected CompileCtx(Map<Class<? extends OptionCtx<?>>, OptionCtx<?>> initialOptions) {
        options = initialOptions;
    }

    protected CompileCtx() {
    }


    /**
     * @param valueConsumer 这个方法将接收两个参数 ({@link DataType} dataType,{@link Object}value)
     *                      如果 dataType为null, 则说明, value 是一个内联命令, 且用户没有指定转化为的Value的存储数据类型(DataType)
     *                      如果 dataType有值, value 可能是一个内联命令,也可能是一个合法的存储数据类型
     *                      这个value可能是解析完成json串, 也可能是内联命令. 如果该方法返回true,则继续解析下一个token, 以此类推
     */
    protected void compileJsonValues(BiFunction<DataType, ?, Boolean> valueConsumer) throws CommandCompileException, ArrayIndexOutOfBoundsException {
        @SuppressWarnings("unchecked")
        BiFunction<DataType, Object, Boolean> vc = (BiFunction<DataType, Object, Boolean>) valueConsumer;
        while (true) {
            String token = stream.token();
            DataType valueType = DataType.typeOfSymbol(token);
            if (valueType != null) {
                try {
                    token = stream.nextToken();
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new CommandCompileException("data type symbol should note a value." + stream.errToken(token));
                }
                Object value;
                try {
                    value = JsonService.parse(token, valueType.reflect().clazz());
                } catch (JsonProcessingException e) {
                    SupplierCtx supplierCtx = compileInlineCommand();
                    if (supplierCtx == null) {
                        throw new CommandCompileException("can not parse json(?) '" + token + "' to valid data type." + stream.errToken(token));
                    }
                    if (!vc.apply(valueType, supplierCtx)) {
                        return;
                    }
                    continue;
                }
                if (!vc.apply(valueType, value)) {
                    stream.next();
                    return;
                }
                stream.next();
                continue;
            }

            // 尝试转换一下
            Object value;
            try {
                value = JsonService.parse(token);
            } catch (JsonProcessingException e) {
                // 可能是内联命令
                SupplierCtx supplierCtx = compileInlineCommand();
                if (supplierCtx == null) {
                    throw new IllegalValueException("can not '" + name() + "' value '" + token + "'." + stream.errToken(token));
                }
                if (!vc.apply(null, supplierCtx)) {
                    return;
                }
                continue;
            }
            DataType supposedType = DataType.typeOfRawValue(value);
            // 期望类型 是 集合类型, 且可变
            // 如果期望的类型是反序列化后返回的类型的父类, 则直接使用
            if (supposedType.reflect().clazz().isAssignableFrom(value.getClass())) {
                if (!vc.apply(supposedType, value)) {
                    stream.next();
                    return;
                }
                stream.next();
                continue;
            }

            // 如果序列化的值不满足需求, 则报错
            throw new IllegalValueException("json value: " + token + " is unsupported to store in database");
        }
    }

    public abstract Class<?> supplyType();

    protected static String normalizeToQueryKey(Object unknownKey) throws UnsupportedQueryKey {
        Object oneValue = normalizeToOneValueOrElseThrow(unknownKey);
        if (oneValue instanceof String str) {
            if (str.isEmpty()) {
                throw new UnsupportedQueryKey("can not query key of element '" + str + "'");
            }
        }
        DataType type;
        try {
            type = DataType.typeOfRawValue(oneValue);
        } catch (IllegalJavaClassStoredException e) {
            log.error("can not normalize unknownKey: {}", unknownKey);
            throw new UnsupportedQueryKey("can not normalize unknownKey: " + unknownKey);
        }
        return switch (type) {
            case STRING, NUMBER -> oneValue.toString();
            default -> throw new UnsupportedQueryKey("can not parse data type '" + type + "' to query key");
        };
    }

    protected static Object normalizeToOneValue(Object unknownValue) {
        try {
            return normalizeToOneValueOrElseThrow(unknownValue);
        } catch (DBInnerException e) {
            return unknownValue;
        }
    }

    protected static Object normalizeToOneValueOrElseThrow(Object unknownValue) {
        Function<Collection<?>, Object> normalizeCollectionToQueryKey = collection -> {
            if (collection.isEmpty()) {
                return "";
            }
            if (collection.size() == 1) {
                Object toQuery = collection.stream().limit(1).toArray()[0];
                return normalizeToOneValueOrElseThrow(toQuery);
            }
            throw new UnsupportedQueryKey("can not query key of a collection containing multiple elements");
        };

        if (unknownValue instanceof Collection<?> co) {
            return normalizeCollectionToQueryKey.apply(co);
        }
        try {
            DataType.typeOfRawValue(unknownValue);
        } catch (IllegalJavaClassStoredException e) {
            log.error("can not normalize : {}", unknownValue);
            throw new DBInnerException("can not normalize: " + unknownValue);
        }
        return unknownValue;
    }

    abstract public Keyword<?> name();

    protected Object consumeWithConsumer(Object suppliedResult) {
        // 没有 Consumer 任务需要消费 Supplier 生产的结果
        if (consumerCtx == null) {
            return suppliedResult;
        }
        // 如果有ConsumerKeyword生成的消费者上下文, 则将结果交由其处理
        return consumerCtx.consume(suppliedResult);
    }

    @NotNull
    protected Object getSuppliedValue(@NotNull SupplierCtx supplierCtx) {
        Object o = supplierCtx.compileResult().get();
        if (o == null) {
            throw new CommandExecuteException("common '" + stream.command() + "' can not receive null value return form inline common '" +
                    supplierCtx.command() + "'." + stream.errToken(supplierCtx.command()));
        }
        return o;
    }

    protected Object adaptSuppliedValueToOneRawValue(Object mayBeSupplier){
        if(mayBeSupplier instanceof SupplierCtx s) {
            return getSuppliedValue(s);
        }
        return mayBeSupplier;
    }

    @Nullable
    protected SupplierCtx compileInlineCommand() throws ArrayIndexOutOfBoundsException {
        String token = stream.token();
        if (token.charAt(0) != '(') {
            return null;
        }
        var inlineCmdCtx = SupplierKeyword.getCompileCtxConstructor(token.substring(token.lastIndexOf("(")+1));
        // 如果不是内联命令, 则返回null
        if (inlineCmdCtx == null) {
            return null;
        }
        // 有其它关键字, 那么有可能下一段是另一串命令,
        var tokenItr = stream.descendingTokenItr();
        int rightParenthesisTokenIndex = tokenItr.cursor();
        // 如果这个右括号不在当前这个token里, 就去找
        if (token.charAt(token.length() - 1) != ')') {
            boolean matched = false;
            while (tokenItr.hasNext()) {
                String nextToken = tokenItr.next();
                if (nextToken.charAt(nextToken.length() - 1) != ')') {
                    continue;
                }
                rightParenthesisTokenIndex = tokenItr.cursor();
                matched = true;
                break;
            }
            // 没找到右括号, 那么就抛出异常
            if (!matched) {
                throw new CommandCompileException("can not find right parenthesis ')' for inline command." + stream.errToken(token));
            }
        }
        beforeCompileInlineCommand();
        var inlineStream = stream.forkSupplierCompileStream(stream.cursor(), rightParenthesisTokenIndex);
        var inlineCommandCtx = inlineStream.compile();
        // 从右括号的下一块开始解析
        stream.jumpTo(rightParenthesisTokenIndex + 1);
        return inlineCommandCtx;
    }

    protected void beforeCompileInlineCommand() {
    }

    /**
     * 判断是否是配置项
     * 否, 则直接返回,不进行编译
     * 是, 则一直向后方编译, 如果遇到管道符, 则fork出 {@link ConsumerCompileStream} 来编译
     */
    protected boolean compileOptions(Function<OptionCtx<?>, Boolean> optionConsumer) {
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
        while (true) {
            if (compilePipe()) {
                return true;
            }
            var optionCtx = Options.compile(token, stream);
            if (optionCtx == null) {
                return false;
            }
            if (!optionConsumer.apply(optionCtx)) {
                return false;
            }
            try {
                token = stream.nextToken();
            } catch (ArrayIndexOutOfBoundsException e) {
                return true;
            }
        }
    }

    protected boolean compilePipe() throws ArrayIndexOutOfBoundsException {
        String token = stream.token();
        // 如果token是管道, 则将编译任务交由 ConsumerCompileStream执行
        if (!ConsumerKeyword.PIPE.match(token)) {
            return false;
        }
        beforeCompilePipe();
        // 跳过管道符
        stream.next();
        // 从父流fork出一个ConsumerCompileStream, 并将其编译, 得到生产任务上下文
        @SuppressWarnings("unchecked")
        ConsumerCtx<Object> consumerCtx = (ConsumerCtx<Object>) stream.forkConsumerCompileStream(
                stream.cursor(),
                stream.tokenSize() - 1,
                this
        ).compile();
        this.consumerCtx = consumerCtx;
        return true;
    }

    public String command() {
        return stream.command();
    }

    /**
     * Hook {@link #compileOptions(Function optionConsumer)}
     * 在开始编译后续的管道前, 进行一些校验工作, 避免编译开销过大
     */
    protected void beforeCompilePipe() {
    }

    @SuppressWarnings("unchecked")
    public <T, O extends OptionCtx<T>> O getOption(Class<O> optionClass) {
        if (options == null) {
            return null;
        }
        return (O) options.get(optionClass);
    }

    @SuppressWarnings("unchecked")
    protected OptionCtx<?> addOption(OptionCtx<?> option) {
        if (options == null) {
            options = new HashMap<>();
        }
        return options.put((Class<? extends OptionCtx<?>>) option.getClass(), option);
    }

    protected void filterAllKeywords() throws ArrayIndexOutOfBoundsException {
        filterAllKeywords(k -> new CommandCompileException("unexpected keyword: '" + k.name() + "'." + stream.errToken(k.name())));
    }

    protected void filterAllKeywords(Function<Keyword<?>, DBExternalException> exceptionSupplier) throws ArrayIndexOutOfBoundsException {
        String token = stream.token();
        var supplierKeyword = SupplierKeyword.typeOfIgnoreCase_(token);
        if (supplierKeyword != null) {
            throw exceptionSupplier.apply(supplierKeyword);
        }
        var consumerKeyword = ConsumerKeyword.typeOfIgnoreCase_(token);
        if (consumerKeyword != null) {
            throw exceptionSupplier.apply(consumerKeyword);
        }
    }

    public void filterAllOptions() {
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DBInnerException(e);
        }
        if (Options.isOption(token)) {
            throw new CommandCompileException("keyword '" + name() + "' can not support any options");
        }
    }
}
