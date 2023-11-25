package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.data.task.UnmodifiedChecker;
import org.hashdb.ms.exception.StopComplieException;

import java.util.List;

/**
 * Date: 2023/11/25 2:45
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class RPushCtx extends MutationCtx {

    protected RPushCtx(CmdCtx<?> fatherCmdCtx) {
        super(fatherCmdCtx);
    }

    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.RPUSH;
    }

    @Override
    protected OpsConsumerTask<List<?>, ?> compile() throws StopComplieException {
        return null;
    }
}
