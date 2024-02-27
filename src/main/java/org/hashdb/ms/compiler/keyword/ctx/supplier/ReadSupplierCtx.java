package org.hashdb.ms.compiler.keyword.ctx.supplier;

import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.LikePatternSyntaxException;
import org.hashdb.ms.compiler.keyword.KeywordModifier;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.exception.UnsupportedQueryKey;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Date: 2023/11/28 20:01
 *
 * @author huanyuMake-pecdle
 */
// TODO: 2024/1/14 要支持读取R(string)原始字符串, 以及like操作
public abstract class ReadSupplierCtx extends SupplierCtx {
    /**
     * 为什么要用Object?
     * 因为这里的key由要么已经被编译生成的
     * 既可能是字符串,也可能是 SupplierCtx (这儿些Ctx是由内联命令(子流)编译而成)
     */
    protected final List<Object> keyOrSuppliers = new LinkedList<>();
    protected boolean like = false;

    @Override
    public @NotNull Class<?> supplyType() {
        return UnmodifiableCollections.unmodifiableList;
    }

    @Override
    protected Supplier<?> compile() {
        doCompile();
        if (keyOrSuppliers.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one key to query");
        }
        return executor();
    }

    @Override
    public Supplier<?> executor() {
        return () -> {
            if (like) {
                // 需要模糊匹配
                var patternOrSupplier = keyOrSuppliers.getFirst();
                Pattern pattern;
                if (patternOrSupplier instanceof SupplierCtx supplierCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    try {
                        pattern = Pattern.compile(normalizeToQueryKey(exeSupplierCtx(supplierCtx)));
                    } catch (PatternSyntaxException e) {
                        throw new LikePatternSyntaxException(e);
                    }
                } else {
                    pattern = ((Pattern) patternOrSupplier);
                }
                return doQueryLike(pattern);
            }
            // 需要批量查询
            return keyOrSuppliers.stream().map(keyOrSupplier -> {
                String key;
                if (keyOrSupplier instanceof SupplierCtx supplierCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    keyOrSupplier = exeSupplierCtx(supplierCtx);
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

    protected List<?> doQueryLikeParameter(Pattern pattern) {
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
                token = stream().token();
            } catch (ArrayIndexOutOfBoundsException e) {
                // 所有token已解析完毕
                return;
            }

            // 如果从这个token开始, 可以编译为一个内联命令, 则将token更新为内联命令执行完的token的下一个token
            CompileCtx<?> inlineSupplierCtx = compileInlineCommand();
            if (inlineSupplierCtx != null) {
                keyOrSuppliers.add(inlineSupplierCtx);
                try {
                    token = stream().token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            }

            // 如果这个token是修饰符, 则将token更新为修饰符编译完的token的下一个token
            if (compileModifier()) {
                try {
                    token = stream().token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    // 解析完成
                    return;
                }
            }

            // 判断是不是配置项, 如果是, 则从现在的token开始, 将所有配置项编译完成后退出
            if (compileOptions(optionCtx -> {
                if (keyOrSuppliers.isEmpty()) {
                    throw new CommandCompileException("key '" + name() + "' require key name to query." + stream().errToken(stream().token()));
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
                    keyOrSuppliers.add(pattern);
                } catch (PatternSyntaxException e) {
                    throw new LikePatternSyntaxException(e);
                }
            } else {
                if (!compileParameter(false, (dataType, parameter) -> {
                    if (dataType != null) {
                        throw new CommandCompileException("the key to query should be a raw string or an legal inline command." + stream().errToken(parameter.getParameterName()));
                    }
                    keyOrSuppliers.add(parameter);
                    return false;
                })) {
                    isOriginalString(token);
                    keyOrSuppliers.add(token);
                }
            }
            // 下一个token
            stream().next();
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (keyOrSuppliers.isEmpty()) {
            throw new CommandCompileException("key '" + name() + "' require key name to query." + stream().errToken(stream().token()));
        }
    }

    protected boolean compileModifier() {
        KeywordModifier modifier;
        try {
            modifier = KeywordModifier.of(stream().token());
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
        switch (modifier) {
            case null -> {
                return false;
            }
            case LIKE -> {
                beforeCompileModifier(KeywordModifier.LIKE);
                stream().next();
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
        Supplier<String> errorMsgSupplier = () -> "modifier keyword 'LIKE' should modify with keyword '" + name() + "'." + stream().errToken(stream().token());
        // 如果已经被修饰过了
        if (like) {
            throw new CommandCompileException(errorMsgSupplier.get());
        }
        // 模糊匹配 key
        String lastToken = stream().peekToken(-1, i -> new CommandCompileException(errorMsgSupplier.get()));
//        if (!SupplierKeyword.GET.match(lastToken)) {
//            throw new CommandCompileException(errorMsgSupplier.get());
//        }
        like = true;
    }

    private record KeyPattern(Pattern pattern, boolean isParameter) {
    }
}
