package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.data.task.UnmodifiedChecker;
import org.hashdb.ms.exception.StopComplieException;

import java.util.List;

/**
 * Date: 2023/11/26 2:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class MutationCtx extends ConsumerCtx<List<?>> {
    protected MutationCtx(CmdCtx<?> fatherCmdCtx) {
        super(fatherCmdCtx);
    }

    @Override
    protected boolean checkConsumeType(Class<?> supplierType) {
        boolean isList = List.class.isAssignableFrom(supplierType);
        if(isList) {
            return UnmodifiedChecker.isUnmodified(supplierType);
        }
        return false;
    }
}
