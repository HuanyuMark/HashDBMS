package org.hashdb.ms.compiler.keyword.ctx.supplier;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.KeyStringModifier;
import org.hashdb.ms.compiler.keyword.SupplierKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.PipeCtx;
import org.hashdb.ms.compiler.option.ExistsOptionContext;
import org.hashdb.ms.compiler.option.LimitOptionContext;
import org.hashdb.ms.compiler.option.OptionContext;
import org.hashdb.ms.data.OpsTask;
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
    protected OpsTask<?> compile(SupplierCompileStream stream) {
        doCompile();
        var limitOption = getOption(LimitOptionContext.class);
        var existOption = getOption(ExistsOptionContext.class);
        return OpsTask.of(() -> {
            Object result;
            if (like) {
                // 需要模糊匹配
                var obj = queryParam.getFirst();
                var pattern = CmdCtx.normalizeToQueryKey(obj);
                result = stream.db().getLikeTask(pattern, limitOption == null ? null : limitOption.value()).get();

//                if (stream.opsTarget() instanceof Database db) {
//                } else {
//                    log.error("unsupported opsTarget: {}", stream.opsTarget());
//                    throw new DBInnerException("unsupported opsTarget");
//                }
            } else {
                // 需要批量查询
                result = queryParam.stream().map(toQuery -> {
                    String queryKey;
                    if (toQuery instanceof SupplierCtx cmdCtx) {
                        // 有内联命令, 则执行其命令, 获取返回结果
                        var inlineCommandResult = cmdCtx.compileResult().get();
                        queryKey = CmdCtx.normalizeToQueryKey(inlineCommandResult);
                    } else {
                        queryKey = ((String) toQuery);
                    }
//                    if (stream.opsTarget() instanceof Database db) {
                        return stream.db().getTask(queryKey).get();
//                    } else {
//                        log.error("unsupported opsTarget: {}", stream.opsTarget());
//                        throw new DBInnerException("unsupported opsTarget");
//                    }
                }).toList();
            }
            // 没有 Consumer 任务需要消费 Supplier 生产的结果
            if (consumerCtx == null) {
                return result;
            }
            // 如果有ConsumerKeyword生成的消费者上下文, 则将结果交由其处理
            // 如果不需要进行入参非空检查
            if (existOption == null || !existOption.value()) {
                return ((ConsumerCtx<Object>) consumerCtx).compileResult(result).get();
            }
            // 需要进行非空检查
            if (result == null) {
                return null;
            }
            // 入参不为空
            return ((ConsumerCtx<Object>) consumerCtx).compileResult(result).get();
        });
    }

    protected void doCompile() {
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                // 所有token已解析完毕
                return;
            }

            // 是不是 提供型关键字
            var supplierCtx = SupplierKeyword.getCmdCtxConstructor(token);
            if (supplierCtx != null) {
                throw new CommandCompileException("can not use keyword as key to query." + stream.errToken(token));
            }

            // 如果从这个token开始, 可以编译为一个内联命令, 则将token更新为内联命令执行完的token的下一个token
            CmdCtx<?> inlineSupplierCtx = compileInlineCommand(token);
            if (inlineSupplierCtx != null) {
                queryParam.add(inlineSupplierCtx);
                try {
                    token = stream.token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    return;
                }
            }

            // 如果这个token是修饰符, 则将token更新为修饰符编译完的token的下一个token
            if (compileModifier(token)) {
                try {
                    token = stream.token();
                } catch (ArrayIndexOutOfBoundsException e) {
                    // 解析完成
                    return;
                }
            }

            // 判断是不是配置项, 如果是, 则从现在的token开始, 将所有配置项编译完成后退出
            if (compileOptions(token)) {
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
            throw new CommandCompileException("key '" + name() + "' require key name to query."+stream.errToken());
        }
    }

    @Override
    protected void beforeAddOption(OptionContext<?> option) {
        if (queryParam.isEmpty()) {
            throw new CommandCompileException("key '" + name() + "' require key name to query."+stream.errToken());
        }
    }

    protected boolean compileModifier(String token) {
        var modifier = KeyStringModifier.of(token);
        switch (modifier) {
            case null -> {
                return false;
            }
            case LIKE -> {
                Supplier<String> errorMsgSupplier = () -> "modifier keyword 'LIKE' should modify with keyword 'GET'." + stream.errToken(token);
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
