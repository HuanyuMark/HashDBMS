package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.keyword.KeywordModifier;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.LikePatternSyntaxException;
import org.hashdb.ms.exception.UnsupportedQueryKey;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Date: 2023/11/28 20:01
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class ReadSupplierCtx extends SupplierCtx {
    /**
     * 为什么要用Object?
     * 因为这里的key由要么已经被编译生成的
     * 既可能是字符串,也可能是 OpsTask (这儿些Task是由内联)
     */
    protected final List<Object> keyOrSupplier = new LinkedList<>();
    protected boolean like = false;

    @Override
    public Class<?> supplyType() {
        return ImmutableChecker.unmodifiableList;
    }

    @Override
    protected Supplier<?> compile() {
        doCompile();
        if (keyOrSupplier.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key to query");
        }
        var limitOption = getOption(LimitOpCtx.class);
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            if (like) {
                // 需要模糊匹配
                var patternOrSupplier = keyOrSupplier.getFirst();
                Pattern pattern;
                if (patternOrSupplier instanceof SupplierCtx supplierCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    try {
                        pattern = Pattern.compile(normalizeToQueryKey(getSuppliedValue(supplierCtx)));
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                } else {
                    pattern = ((Pattern) patternOrSupplier);
                }
                return doQueryLike(pattern);
            }
            // 需要批量查询
            return keyOrSupplier.stream().map(keyOrSupplier -> {
                String key;
                if (keyOrSupplier instanceof SupplierCtx supplierCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    keyOrSupplier = getSuppliedValue(supplierCtx);
                    try {
                        key = normalizeToQueryKey(keyOrSupplier);
                    } catch (UnsupportedQueryKey e) {
                        throw UnsupportedQueryKey.of(name(), supplierCtx);
                    }
                } else {
                    key = ((String) keyOrSupplier);
                }

                return doQuery(key);
            }).toList();
        };
    }

    protected List<?> doQueryLike(Pattern pattern) {
        throw new UnsupportedOperationException();
    }

    abstract protected Object doQuery(String key);

    protected void doCompile() {
        while (true) {
            String token;
            try {
                if (compilePipe()) {
                    return;
                }
                // 是不是关键字
                filterAllKeywords();
                filterAllOptions();
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                // 所有token已解析完毕
                return;
            }

            // 如果从这个token开始, 可以编译为一个内联命令, 则将token更新为内联命令执行完的token的下一个token
            CompileCtx<?> inlineSupplierCtx = compileInlineCommand();
            if (inlineSupplierCtx != null) {
                keyOrSupplier.add(inlineSupplierCtx);
                try {
                    token = stream.token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            }

            // 如果这个token是修饰符, 则将token更新为修饰符编译完的token的下一个token
            if (compileModifier()) {
                try {
                    token = stream.token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    // 解析完成
                    return;
                }
            }

            // 判断是不是配置项, 如果是, 则从现在的token开始, 将所有配置项编译完成后退出
            if (compileOptions(optionCtx -> {
                if (keyOrSupplier.isEmpty()) {
                    throw new CommandCompileException("key '" + name() + "' require key name to query." + stream.errToken(stream.token()));
                }
                addOption(optionCtx);
                return true;
            })) {
                return;
            }
            if (like) {
                try {
                    // 提前编译, 并且校验语法错误
                    Pattern pattern = Pattern.compile(token);
                    keyOrSupplier.add(pattern);
                } catch (PatternSyntaxException e) {
                    throw new LikePatternSyntaxException(e);
                }
            } else {
                keyOrSupplier.add(token);
            }
            // 下一个token
            stream.next();
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (keyOrSupplier.isEmpty()) {
            throw new CommandCompileException("key '" + name() + "' require key name to query." + stream.errToken(stream.token()));
        }
    }

    protected boolean compileModifier() {
        KeywordModifier modifier;
        try {
            modifier = KeywordModifier.of(stream.token());
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
        switch (modifier) {
            case null -> {
                return false;
            }
            case LIKE -> {
                beforeCompileModifier(KeywordModifier.LIKE);
                stream.next();
                return true;
            }
            default ->
                    throw new UnsupportedOperationException("keyword '" + name() + "' can not be modified by '" + modifier + "'");
        }
    }

    protected void beforeCompileModifier(KeywordModifier modifier) {
        if (modifier != KeywordModifier.LIKE) {
            return;
        }
        Supplier<String> errorMsgSupplier = () -> "modifier keyword 'LIKE' should modify with keyword '" + name() + "'." + stream.errToken(stream.token());
        // 如果已经被修饰过了
        if (like) {
            throw new CommandCompileException(errorMsgSupplier.get());
        }
        // 模糊匹配 key
        String lastToken = stream.peekToken(-1, i -> new CommandCompileException(errorMsgSupplier.get()));
//        if (!SupplierKeyword.GET.match(lastToken)) {
//            throw new CommandCompileException(errorMsgSupplier.get());
//        }
        like = true;
    }
}
