package org.hashdb.ms.compiler.keyword.ctx;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.Keyword;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.OptionContext;
import org.hashdb.ms.compiler.option.Options;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.hashdb.ms.util.JacksonSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Date: 2023/11/25 0:34
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class CmdCtx<S extends TokenCompileStream> {
    protected final Map<Class<? extends OptionContext<?>>, OptionContext<?>> options;


    protected S stream;
    /**
     * 是否有管道符? 如果有, 则会有管道Ctx, 其会包装其它的 ConsumerCtx
     * 在当前 SupplierCtx的任务执行完后, 会接收这个Supplier 产生的结果
     * 当作ConsumerCtx的操作对象, 进行消费
     */
    protected ConsumerCtx<?> consumerCtx;

    protected CmdCtx(Map<Class<? extends OptionContext<?>>, OptionContext<?>> initialOptions) {
        options = initialOptions;
    }

    protected CmdCtx() {
        options = new HashMap<>();
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
                throw new UnsupportedQueryKey("can not query key of element '" + toQuery + "' of collection '" + JacksonSerializer.jsonfy(collection) + "'");
            }
            throw new UnsupportedQueryKey("can not query key of a collection containing multiple elements");
        };

        if (unknownKey instanceof Collection<?> toQueryCollection) {
            return normalizeCollectionToQueryKey.apply(toQueryCollection);
        }
        DataType type = DataType.typeOfRawValue(unknownKey);
        return switch (type) {
            case STRING, NUMBER -> unknownKey.toString();
            case NULL -> "";
            default -> throw new DBInnerException();
        };
    }

    abstract public Keyword name();

    @Nullable
    protected SupplierCtx compileInlineCommand(@NotNull String token) {
        if (token.charAt(0) != '(') {
            return null;
        }
        var inlineCmdCtx = SupplierKeyword.getCmdCtxConstructor(token.substring(1));
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
            var inlineStream = stream.forkSupplierCompileStream(stream.cursor(), rightParenthesisTokenIndex);
            var inlineCommandCtx = inlineStream.compile();
            // 从右括号的下一块开始解析
            stream.jumpTo(rightParenthesisTokenIndex + 1);
            return inlineCommandCtx;
        }
        return null;
    }

    /**
     * 判断是否是配置项
     * 否, 则直接返回,不进行编译
     * 是, 则一直向后方编译, 如果遇到管道符, 则fork出 {@link ConsumerCompileStream} 来编译
     *
     * @param token 未知token
     */
    protected boolean compileOptions(String token) {
        while (true) {
            // 如果token是管道, 则将编译任务交由 ConsumerCompileStream执行
            if (ConsumerKeyword.PIPE.match(token)) {
                beforeCompilePipe();
                // 跳过管道符
                stream.next();
                // 从父流fork出一个ConsumerCompileStream, 并将其编译, 得到生产任务上下文
                this.consumerCtx = stream.forkConsumerCompileStream(
                        stream.cursor(),
                        stream.tokenSize() - 1,
                        this
                        ).compile();
                return true;
            }
            var optionCtx = Options.compile(token, stream);
            if (optionCtx == null) {
                return false;
            }
            addOption(optionCtx);
            try {
                token = stream.nextToken();
            } catch (ArrayIndexOutOfBoundsException e) {
                return true;
            }
        }
    }

    public void filterAllOptions(){
        String token;
        try {
            token = stream.token();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new DBInnerException(e);
        }
        if (Options.isOption(token)) {
            throw new CommandCompileException("keyword '"+name()+"' can not support any options");
        }
    }

    /**
     * Hook {@link #compileOptions(String token)}
     * 在开始编译后续的管道前, 进行一些校验工作, 避免编译开销过大
     */
    protected void beforeCompilePipe() {
    }

    /**
     * Hook {@link #addOption(OptionContext optionCtx)}
     * 在加入options 列表前 进行一些校验工作
     */
    protected void beforeAddOption(OptionContext<?> option) {
    }

    @SuppressWarnings("unchecked")
    public <T, O extends OptionContext<T>> O getOption(Class<O> optionClass) {
        return (O) options.get(optionClass);
    }

    @SuppressWarnings("unchecked")
    protected OptionContext<?> addOption(OptionContext<?> option) {
        beforeAddOption(option);
        return options.put((Class<? extends OptionContext<?>>) option.getClass(), option);
    }

    protected void filterAllKeywords() {
        // TODO: 2023/11/26 筛掉要用到的所有keyword
    }
}
