package org.hashdb.ms.compiler.keyword.ctx.consumer;

import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.option.OptionContext;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.OpsTask;
import org.hashdb.ms.exception.StopComplieException;

/**
 * Date: 2023/11/25 0:30
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class PipeCtx extends ConsumerCtx<Object> {

    public PipeCtx(CmdCtx<?> supplierCtx, DataType supportedType) {
        super(supplierCtx);
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.PIPE;
    }

    @Override
    protected boolean checkConsumeType(Class<?> supplierClass) {
        return supplierClass != null;
    }

    @Override
    public Class<?> supplyType() {
        return Object.class;
    }

    @Override
    public <T, O extends OptionContext<T>> O getOption(Class<O> optionClass) {
        return null;
    }

    @Override
    protected OpsConsumerTask<Object, ?> compile() throws StopComplieException {
        doCompile();

        return (opsTarget) -> {

            return OpsTask.of(() -> {

                return null;
            });
        };
    }

    protected void doCompile() throws StopComplieException {
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            ConsumerCtx<?> objectConsumerCtx = ConsumerKeyword.createCtx(token, fatherCmdCtx);
//            fatherCmdCtx.supplyType()

        }
    }
}
