package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.list.LGetGtx;

import java.util.List;

/**
 * Date: 2023/11/29 11:42
 * 因为语义重复的问题(多个ConsumerCtx里可能都有这个 Get 关键字), 所以这个 GetCtx 主要的职责是,
 * 在编译时, 确认使用哪个数据结构的 Get 关键字, 在编译时能确认是哪个关键字, 运行时就运行哪个关键字
 * 否则运行时编译, 效率低, 其它在这个包下的关键字, 基本都有这个问题, 职责也与这个类相似
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class GetCtx extends InterpretCtx {
    protected GetCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx, List.of(
                new LGetGtx(fatherCompileCtx)
        ));
    }
    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.GET;
    }
}
