package org.hashdb.ms.compiler.keyword.ctx;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.exception.*;
import org.hashdb.ms.compiler.keyword.CompilerNode;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.Keyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.JsonValueCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.ParameterCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.exception.DBClientException;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.UnsupportedQueryKey;
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
 */
@Slf4j
public abstract class CompileCtx<S extends DatabaseCompileStream> implements CompilerNode {
    protected Map<Class<? extends OptionCtx<?>>, OptionCtx<?>> options;

    protected int costExpectant;
    @JsonIgnore
    private S stream;
    /**
     * 是否有管道符? 如果有, 则会有管道Ctx, 其会包装其它的 ConsumerCtx
     * 在当前 SupplierCtx的任务执行完后, 会接收这个Supplier 产生的结果
     * 当作ConsumerCtx的操作对象, 进行消费
     */
    @JsonProperty
    protected ConsumerCtx<Object> consumerCtx;

    protected CompileCtx(Map<Class<? extends OptionCtx<?>>, OptionCtx<?>> initialOptions) {
        options = initialOptions;
    }

    protected CompileCtx() {
    }


    /**
     * @param valueChecker 这个方法将接收两个参数 ({@link DataType} dataType,{@link Object}value)
     *                     如果 dataType为null, 则说明, value 是一个内联命令, 且用户没有指定转化为的Value的存储数据类型(DataType)
     *                     如果 dataType有值, value 可能是一个内联命令,也可能是一个合法的存储数据类型
     *                     这个value可能是解析完成json串, 也可能是内联命令. 如果该方法返回true,则继续解析下一个token, 以此类推
     */
    protected void compileJsonValues(BiFunction<DataType, SupplierCtx, Boolean> valueChecker) throws CommandCompileException, ArrayIndexOutOfBoundsException {
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
                        throw new CommandCompileException("can not parse json(?) '" + token + "' to type '" + valueType + "'." + stream.errToken(token));
                    }
                    if (!valueChecker.apply(valueType, supplierCtx)) {
                        return;
                    }
                    continue;
                }

                if (!valueChecker.apply(valueType, new JsonValueCtx(valueType, value))) {
                    stream.next();
                    return;
                }
                stream.next();
                continue;
            }

            // 尝试转换一下
            @Nullable
            Object value;
            try {
                value = JsonService.parse(token);
            } catch (JsonProcessingException e) {
                // 可能是内联命令
                SupplierCtx supplierCtx = compileInlineCommand();
                if (supplierCtx == null) {
                    throw new IllegalValueException("can not '" + name() + "' value '" + token + "'." + stream.errToken(token));
                }
                if (!valueChecker.apply(null, supplierCtx)) {
                    return;
                }
                continue;
            }
            DataType supposedType = DataType.typeOfRawValue(value);
            // 期望类型 是 集合类型, 且可变
            // 如果期望的类型是反序列化后返回的类型的父类, 则直接使用
//            if (!supposedType.support(value)) {// 如果序列化的值不满足需求, 则报错
//                throw new IllegalValueException("json value: '" + token + "' is unsupported to store in database");
//            }

            if (!valueChecker.apply(supposedType, new JsonValueCtx(supposedType, value))) {
                stream.next();
                return;
            }
            stream.next();
        }
    }

    /**
     * @param store        这个参数是否参与存储过程。也即是这个参数的值是否会被存入参数集中
     * @param valueChecker 若DataType不为空, 则解析出的参数为jsonValue否则为内联命令
     * @return 如果在编译过程中, 编译到有token是参数, 则返回true, 否则返回false
     * 如果是参数, 则会获取当前session里的参数, 如果没有, 则抛出{@link NotFoundParameterException} 异常
     * @throws NotFoundParameterException 如果找不到参数, 则抛出异常
     */
    // TODO: 2024/1/13 这里应该还有些问题:
    // 如果这条命令被缓存后, 用到的参数在参数集中被删除, 那么这条被缓存的命令理应过期
    protected boolean compileParameter(boolean store, BiFunction<DataType, ParameterCtx, Boolean> valueChecker) throws ArrayIndexOutOfBoundsException, NotFoundParameterException {
        boolean match = false;
        while (true) {
            String token = stream.token();
            if (!token.startsWith("$")) {
                return match;
            }
            var parameterAccessorCtx = new ParameterCtx(stream, token, store);
            parameterAccessorCtx.compileWithStream((SupplierCompileStream) stream);
            match = true;
            // 登记该组流使用该参数, 防止参数变更后缓存的编译流过期
            if (parameterAccessorCtx.value() instanceof SupplierCtx && !valueChecker.apply(null, parameterAccessorCtx)) {
                return true;
            }
            if (!valueChecker.apply(parameterAccessorCtx.storeType(), parameterAccessorCtx)) {
                return true;
            }
        }
    }

    @NotNull
    public abstract Class<?> supplyType();

    @NotNull
    protected static String normalizeToQueryKey(Object unknownKey) throws UnsupportedQueryKey {
        Object oneValue = selectOneKeyOrElseThrow(unknownKey);
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

    @NotNull
    protected static Object selectOneKeyOrElseThrow(Object unknownValue) {
        Function<Collection<?>, Object> normalizeCollectionToQueryKey = collection -> {
            if (collection.isEmpty()) {
                return "";
            }
            if (collection.size() == 1) {
                Object toQuery = collection.stream().limit(1).toArray()[0];
                return selectOneKeyOrElseThrow(toQuery);
            }
            throw new UnsupportedQueryKey("can not query key of a collection containing multiple elements");
        };

        if (unknownValue instanceof Collection<?> co) {
            return normalizeCollectionToQueryKey.apply(co);
        }
        if (unknownValue instanceof HValue<?> hValue) {
            return selectOneKeyOrElseThrow(hValue.data());
        }

        try {
            DataType.typeOfRawValue(unknownValue);
        } catch (IllegalJavaClassStoredException e) {
            log.error("can not normalize : {}", unknownValue);
            throw new CommandExecuteException("can not normalize: " + unknownValue);
        }
        return unknownValue;
    }

    @Override
    abstract public Keyword<?> name();

    protected Object callConsumer(Object consumeValue) {
        // 没有 Consumer 任务需要消费 Supplier 生产的结果
        if (consumerCtx == null) {
            return consumeValue;
        }
        // 如果有ConsumerKeyword生成的消费者上下文, 则将结果交由其处理
        return consumerCtx.consume(consumeValue);
    }

    @NotNull
    protected Object exeSupplierCtx(@NotNull SupplierCtx supplierCtx) {
        return exeSupplierCtx(supplierCtx, false);
    }

    @NotNull
    protected Object exeSupplierCtx(@NotNull SupplierCtx supplierCtx, boolean copy) {
        if (supplierCtx instanceof JsonValueCtx jsonValueCtx) {
            return jsonValueCtx.executor();
        }
        if (supplierCtx instanceof ParameterCtx parameterCtx) {
            return parameterCtx.executor();
        }
        Object o = supplierCtx.compileResult().get();
        if (o == null) {
            throw new CommandExecuteException("common '" + stream.command() + "' can not receive null value return form inline common '" +
                    supplierCtx.command() + "'." + stream.errToken(supplierCtx.command()));
        }
        if (copy) {
            return o;
        }
        // TODO: 2024/1/12 根据SupplierCtx指定的返回类型, 使用DataType里
        // 定义好的clone(Object) 方法克隆, 然后返回
        return supplierCtx.storeType().clone(o);
    }

    protected Object adaptSuppliedValueToOneRawValue(Object mayBeSupplier) {
        if (mayBeSupplier instanceof SupplierCtx s) {
            return exeSupplierCtx(s);
        }
        return mayBeSupplier;
    }

    // TODO: 2024/1/12 让用户指定内联命令的返回值类型
    // 否则在执行期间得到内联命令返回值后, 不知道将这个值
    // clone成什么类型
    @Nullable
    protected SupplierCtx compileInlineCommand() throws ArrayIndexOutOfBoundsException {
        // 如果有DataTypeSymbol
        String mayBeDataTypeSymbol = stream.token();
        DataType storeType = DataType.typeOfSymbol(mayBeDataTypeSymbol);
        if (storeType != null) {
            stream.next();
        }
        String token = stream.token();
        if (token.charAt(0) != '(') {
            if (storeType == null) {
                return null;
            }
            throw new CommandCompileException("DataType Symbol '" + mayBeDataTypeSymbol + "' should modify a value expression");
        }
        var inlineCmdCtx = SupplierKeyword.getCompileCtxConstructor(token.substring(token.lastIndexOf("(") + 1));
        // 如果不是内联命令, 则返回null
        if (inlineCmdCtx == null) {
            return null;
        }
//        {  // 有其它关键字, 那么有可能下一段是另一串命令,
//            var tokenItr = stream.descendingTokenItr();
//            int rightParenthesisTokenIndex = tokenItr.cursor();
//            // 如果这个右括号不在当前这个token里, 就去找
//            if (token.charAt(token.length() - 1) != ')') {
//                boolean matched = false;
//                while (tokenItr.hasNext()) {
//                    String nextToken = tokenItr.next();
//                    if (nextToken.charAt(nextToken.length() - 1) != ')') {
//                        continue;
//                    }
//                    rightParenthesisTokenIndex = tokenItr.cursor();
//                    matched = true;
//                    break;
//                }
//                // 没找到右括号, 那么就抛出异常
//                if (!matched) {
//                    throw new CommandCompileException("can not find ')' for inline command." + stream.errToken(token));
//                }
//            }
//        }
        // 有其它关键字, 那么有可能下一段是另一串命令
        var tokenItr = stream.tokenItr(stream.cursor());
        int rightParenthesisTokenIndex = tokenItr.cursor();
        // 如果这个右括号不在当前这个token里, 就去找
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
            throw new CommandCompileException("can not find ')' for inline command." + stream.errToken(token));
        }
        beforeCompileInlineCommand();
        var inlineStream = stream.forkSupplierCompileStream(stream.cursor(), rightParenthesisTokenIndex);
        var inlineCommandCtx = inlineStream.compile(inlineCmdCtx.create());
        inlineCommandCtx.setStoreType(storeType);
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

    protected void filterAllKeywords(Function<Keyword<?>, DBClientException> exceptionSupplier) throws ArrayIndexOutOfBoundsException {
        String token = stream.token();
        var supplierKeyword = SupplierKeyword.typeOfIgnoreCase(token);
        if (supplierKeyword != null) {
            throw exceptionSupplier.apply(supplierKeyword);
        }
        var consumerKeyword = ConsumerKeyword.typeOfIgnoreCase(token);
        if (consumerKeyword != null) {
            throw exceptionSupplier.apply(consumerKeyword);
        }
    }

    public void filterAllOptions() {
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DBSystemException(e);
        }
        if (Options.getOption(token) != null) {
            throw new CommandCompileException("keyword '" + name() + "' can not support any options");
        }
    }

    public S stream() {
        return stream;
    }

    @JsonIgnore
    public void setStream(S stream) {
        this.stream = stream;
    }

    /**
     * 提取原始字符串表达式 {@code R(string)} 括号中的字符串(这个{@code R}不限大小写)
     * 这里面的字符串不会被视作有其它意义而被替换, 只会当成普通的字符串.
     * <br/>
     * 比如, 如果一个 key 是 {@code $param}, 因为这个形式的字符串是 parameter 参数
     * 的形式, 所以会被替换为参数集中 {@code $param} 的值
     * 但如果这个key 是 {@code  R($param)} 则{@code $param}就会当作普通字符串.
     * 例如下面这段语句, 因为key为{@code  R($param)}, 所以不会被替换为{@code  $param}在参数集中对应的值
     * 而是是为一个字符串, 又因为{@code '$param'}符合parameterName的命名规范, 所以在设置参数时, 就需要使用
     * <br/>
     * {@code
     * SET R($param1) "value1" R($param2) 456
     * }
     * <br/>
     * 的形式
     *
     * @param any 任意字符串
     * @return 如果该字符串是原始字符串, 则提取出原始字符串, 否则返回null
     */
    protected String extractOriginalString(String any) {
        if (!isOriginalString(any)) {
            return null;
        }
        return any.substring(2, any.length() - 1);
    }

    protected boolean isOriginalString(String any) {
        if (any.length() < 3) {
            return false;
        }
        if (any.charAt(0) != 'R' && any.charAt(0) != 'r') {
            return false;
        }
        if (any.charAt(1) != '(') {
            return false;
        }
        int lastIndex = any.length() - 1;
        if (any.charAt(lastIndex) != ')') {
            throw new IllegalExpressionException("uncompleted expression." + stream.errToken(any));
        }
        if (lastIndex - 2 <= 0) {
            throw new CommandCompileException("the param of function 'R(string)' should be not blank." + stream.errToken(any));
        }
        return true;
    }

    public int costExpectant() {
        return costExpectant;
    }
}
