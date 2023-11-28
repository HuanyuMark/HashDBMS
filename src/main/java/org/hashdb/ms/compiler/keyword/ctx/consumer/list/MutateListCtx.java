package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.ConsumerCtx;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.util.JacksonSerializer;

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
        if (!List.class.isAssignableFrom(supplierType.getClass())) {
            return false;
        }
        List<?> list = (List<?>) supplierType;
        if (list.size() != 1) {
            return false;
        }
        Object first = list.getFirst();
        if(!(first instanceof List<?>)) {
            throw new CommandExecuteException("keyword '"+name()+"' can not operate value '"+
                     JacksonSerializer.stringfy(supplierType) +"'");
        }
        return !ImmutableChecker.isUnmodifiableCollection(first.getClass());
    }


}
