package org.hashdb.ms.compiler.keyword.ctx.supplier;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.keyword.KeyStringModifier;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.option.LimitOpCtx;
import org.hashdb.ms.compiler.option.OptionCtx;
import org.hashdb.ms.exception.CommandCompileException;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Date: 2023/11/24 16:20
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
public class GetCtx extends SupplierCtx {

    private boolean like = false;

    /**
     * 为什么要用Object?
     * 因为这里的key由要么已经被编译生成的
     * 既可能是字符串,也可能是 OpsTask (这儿些Task是由内联)
     */
    private final List<Object> queryParam = new LinkedList<>();

    @Override
    public SupplierKeyword name() {
        return SupplierKeyword.GET;
    }

    @Override
    public Class<?> supplyType() {
        return List.class;
    }

    @Override
    protected Supplier<?> compile() {
        doCompile();
        var limitOption = getOption(LimitOpCtx.class);
        return () -> {
            if (like) {
                // 需要模糊匹配
                var toQuery = queryParam.getFirst();
                var pattern = "";
                if (toQuery instanceof SupplierCtx cmdCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    var inlineCommandResult = cmdCtx.compileResult().get();
                    pattern = CompileCtx.normalizeToQueryKey(inlineCommandResult);
                } else {
                    pattern = CompileCtx.normalizeToQueryKey(toQuery);
                }
                return stream.db().getLikeTask(pattern, limitOption == null ? null : limitOption.value()).get();
            }
            // 需要批量查询
            return queryParam.stream().map(toQuery -> {
                String queryKey;
                if (toQuery instanceof SupplierCtx cmdCtx) {
                    // 有内联命令, 则执行其命令, 获取返回结果
                    var inlineCommandResult = cmdCtx.compileResult().get();
                    queryKey = CompileCtx.normalizeToQueryKey(inlineCommandResult);
                } else {
                    queryKey = CompileCtx.normalizeToQueryKey(toQuery);
                }
                return stream.db().getTask(queryKey).get();
            }).toList();
        };
    }

    protected void doCompile() {
        while (true) {
            String token;
            try {
                // 是不是关键字
                filterAllKeywords();
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                // 所有token已解析完毕
                return;
            }
            var supplierCtx = SupplierKeyword.getCompileCtxConstructor(token);
            if (supplierCtx != null) {
                throw new CommandCompileException("can not use keyword as key to query." + stream.errToken(token));
            }

            // 如果从这个token开始, 可以编译为一个内联命令, 则将token更新为内联命令执行完的token的下一个token
            CompileCtx<?> inlineSupplierCtx = compileInlineCommand();
            if (inlineSupplierCtx != null) {
                queryParam.add(inlineSupplierCtx);
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
                addOption(optionCtx);
                return true;
            })) {
                return;
            }

            queryParam.add(token);
            // 下一个token
            stream.next();
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if (queryParam.isEmpty()) {
            throw new CommandCompileException("key '" + name() + "' require key name to query." + stream.errToken());
        }
    }

    @Override
    protected void beforeAddOption(OptionCtx<?> option) {
        if (queryParam.isEmpty()) {
            throw new CommandCompileException("key '" + name() + "' require key name to query." + stream.errToken());
        }
    }

    protected boolean compileModifier() {
        KeyStringModifier modifier;
        try {
            modifier = KeyStringModifier.of(stream.token());
        } catch (ArrayIndexOutOfBoundsException e) {
            return true;
        }
        switch (modifier) {
            case null -> {
                return false;
            }
            case LIKE -> {
                Supplier<String> errorMsgSupplier = () -> "modifier keyword 'LIKE' should modify with keyword 'GET'." + stream.errToken(stream.token());
                // 如果已经被修饰过了
                if (like) {
                    throw new CommandCompileException(errorMsgSupplier.get());
                }
                // 模糊匹配 key
                String lastToken = stream.peekToken(-1, i -> new CommandCompileException(errorMsgSupplier.get()));
                if (!SupplierKeyword.GET.match(lastToken)) {
                    throw new CommandCompileException(errorMsgSupplier.get());
                }
                like = true;
                stream.next();
                return true;
            }
            default ->
                    throw new UnsupportedOperationException("keyword '" + name() + "' can not be modified by '" + modifier + "'");
        }
    }
}
