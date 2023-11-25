package org.hashdb.ms.compiler.keyword.ctx;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.Keyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.task.UnmodifiedChecker;
import org.hashdb.ms.exception.*;
import org.hashdb.ms.util.JacksonSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Date: 2023/11/25 0:34
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public abstract class CompileCtx<S extends TokenCompileStream> {
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

    protected void compileJsonValues(Function<?, Boolean> valueConsumer) throws CommandCompileException {
        @SuppressWarnings("unchecked")
        Function<Object, Boolean> vc = (Function<Object, Boolean>) valueConsumer;
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            try {
                DataType valueType = DataType.typeOfSymbol(token);
                filterAllKeywords();
                filterAllOptions();
                if (valueType != null) {
                    try {
                        token = stream.nextToken();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new CommandCompileException("data type symbol should note a value." + stream.errToken(token));
                    }
                    Object value = JacksonSerializer.parse(token, valueType.reflect().clazz());
                    if (!vc.apply(value)) {
                        stream.next();
                        return;
                    }
                    stream.next();
                    continue;
                }

                // 尝试转换一下
                Object value = JacksonSerializer.parse(token);
                DataType supposedType;
                try {
                    supposedType = DataType.typeOfRawValue(value);
                } catch (IllegalJavaClassStoredException e) {
                    throw new CommandCompileException("can not parse json(?) '" + token + "' to valid data type." + stream.errToken(token));
                }
                if (DataType.NULL == supposedType) {
                    // 可能是内联命令
                    SupplierCtx supplierCtx = compileInlineCommand();
                    if (supplierCtx == null) {
                        throw new IllegalValueException("can not '"+name()+"' value '" + token + "'." + stream.errToken(token));
                    }
                    if (!vc.apply(value)) {
                        stream.next();
                        return;
                    }
                    stream.next();
                    continue;
                }
                // 将不可变对象转为可变对象
                if (UnmodifiedChecker.isUnmodifiableCollection(value.getClass())) {
                    @SuppressWarnings("unchecked")
                    Collection<? super Collection<?>> newCollection = (Collection<? super Collection<?>>) supposedType.reflect().create();
                    Collections.addAll(newCollection, (Collection<?>) value);
                    if (!vc.apply(value)) {
                        stream.next();
                        return;
                    }
                    stream.next();
                    continue;
                }
                // 期望类型 是 集合类型, 且可变
                // 如果期望的类型是反序列化后返回的类型的父类, 则直接使用
                if (supposedType.reflect().clazz().isAssignableFrom(value.getClass())) {
                    if (!vc.apply(value)) {
                        stream.next();
                        return;
                    }
                    stream.next();
                    continue;
                }
                // TODO: 2023/11/26 尝试转换

                // 如果序列化的值不满足需求, 则报错
                throw new IllegalValueException("json value: " + token + " is unsupported to store in database");
            } catch (JsonProcessingException e) {
                throw new CommandCompileException("can not parse json(?) '" + token + "' to valid data type." + stream.errToken(token));
            }
        }
    }

    public abstract Class<?> supplyType();

    protected static String normalizeToQueryKey(Object unknownKey) {
        Function<Collection<?>, String> normalizeCollectionToQueryKey = collection -> {
            if (collection.isEmpty()) {
                return "";
            }
            if (collection.size() == 1) {
                Object toQuery = collection.stream().limit(1).toArray()[0];
                String toQueryKey = normalizeToQueryKey(toQuery);
                if (!toQueryKey.isEmpty()) {
                    return toQueryKey;
                }
                throw new UnsupportedQueryKey("can not query key of element '" + toQuery + "' of collection '" + JacksonSerializer.stringfy(collection) + "'");
            }
            throw new UnsupportedQueryKey("can not query key of a collection containing multiple elements");
        };

        if (unknownKey instanceof Collection<?> toQueryCollection) {
            return normalizeCollectionToQueryKey.apply(toQueryCollection);
        }
        DataType type;
        try {
            type = DataType.typeOfRawValue(unknownKey);
        } catch (IllegalJavaClassStoredException e) {
            log.error("can not normalize unknownKey: {}", unknownKey);
            throw new DBInnerException("can not normalize unknownKey: " + unknownKey);
        }
        return switch (type) {
            case STRING, NUMBER -> unknownKey.toString();
            default -> throw new DBInnerException("can not pase data type '" + type + "' to query key");
        };
    }

    abstract public Keyword name();

    protected Object consumeWithConsumer(Object suppliedResult) {
        // 没有 Consumer 任务需要消费 Supplier 生产的结果
        if (consumerCtx == null) {
            return suppliedResult;
        }
        // 如果有ConsumerKeyword生成的消费者上下文, 则将结果交由其处理
        return consumerCtx.compileResult(suppliedResult).get();
    }

    @Nullable
    protected SupplierCtx compileInlineCommand() {
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
        if (token.charAt(0) != '(') {
            return null;
        }
        var inlineCmdCtx = SupplierKeyword.getCompileCtxConstructor(token.substring(1));
        // 有其它关键字, 那么有可能下一段是另一串命令,
        if (inlineCmdCtx != null) {
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
        return null;
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

    protected boolean compilePipe() {
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
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

    public String command(){
        return stream.command();
    }

    /**
     * Hook {@link #compileOptions(Function optionConsumer)}
     * 在开始编译后续的管道前, 进行一些校验工作, 避免编译开销过大
     */
    protected void beforeCompilePipe() {
    }

    /**
     * Hook {@link #addOption(OptionCtx optionCtx)}
     * 在加入options 列表前 进行一些校验工作
     */
    protected void beforeAddOption(OptionCtx<?> option) {
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
        beforeAddOption(option);
        if (options == null) {
            options = new HashMap<>();
        }
        return options.put((Class<? extends OptionCtx<?>>) option.getClass(), option);
    }

    protected void filterAllKeywords() throws ArrayIndexOutOfBoundsException {
        filterAllKeywords(k->new CommandCompileException("unexpected keyword: '"+k.name()+"'."+stream.errToken(k.name())));
    }

    protected void filterAllKeywords(Function<Keyword<?>,DBExternalException> exceptionSupplier) throws ArrayIndexOutOfBoundsException {
        String token = stream.token();
        var supplierKeyword = SupplierKeyword.typeOfIgnoreCase_(token);
        if(supplierKeyword != null) {
            throw  exceptionSupplier.apply(supplierKeyword);
        }
        var consumerKeyword = ConsumerKeyword.typeOfIgnoreCase_(token);
        if(consumerKeyword != null) {
            throw  exceptionSupplier.apply(supplierKeyword);
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
