package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.task.UnmodifiedChecker;

import java.util.List;

/**
 * Date: 2023/11/26 2:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class MutateListCtx extends ConsumerCtx<List<?>> {
    protected MutateListCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected boolean checkConsumeType(Object supplierType) {
        boolean isList = List.class.isAssignableFrom(supplierType.getClass());
        if(isList) {
            List<?> list = (List<?>) supplierType;
            if(list.size() != 1) {
                return false;
            }
            return !UnmodifiedChecker.isUnmodifiableCollection(list.getFirst().getClass());
        }
        return false;
    }
}
