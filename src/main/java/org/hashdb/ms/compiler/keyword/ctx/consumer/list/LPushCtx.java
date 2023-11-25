package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hashdb.ms.compiler.keyword.ConsumerKeyword;
import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.task.UnmodifiedChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.exception.IllegalValueException;
import org.hashdb.ms.exception.StopComplieException;
import org.hashdb.ms.util.JacksonSerializer;

import java.util.LinkedList;
import java.util.List;

/**
 * Date: 2023/11/25 2:42
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LPushCtx extends MutationCtx {

    private List<Object> values = new LinkedList<>();

    protected LPushCtx(CmdCtx<?> fatherCmdCtx) {
        super(fatherCmdCtx);
    }

    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }

    @Override
    public ConsumerKeyword name() {
        return ConsumerKeyword.LPUSH;
    }

    @Override
    protected OpsConsumerTask<List<?>, ?> compile() throws StopComplieException {
        doCompile();
        return (opsTarget) -> {
            return null;
//            return ListOpsTaskSupplier.lPush((List<?>) opsTarget, )
        };
    }

    private void doCompile() throws StopComplieException {
        while (true) {
            String token;
            try {
                token = stream.token();
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }
            try {
                DataType valueType = DataType.typeOfSymbol(token);
                if (valueType != null) {
                    token = stream.nextToken();
                    filterAllKeywords();
                    filterAllOptions();
                    Object value = JacksonSerializer.parse(token, valueType.defaultJavaClass());
                    values.add(value);
                    stream.next();
                    continue;
                }
                filterAllKeywords();
                filterAllOptions();
                // 尝试转换一下
                Object value = JacksonSerializer.parse(token);
                DataType supposedType;
                try {
                    supposedType = DataType.typeOfRawValue(value);
                } catch (IllegalJavaClassStoredException e) {
                    throw new CommandCompileException("can not parse json '" + token + "' to valid data type." + stream.errToken(token));
                }
                if(DataType.NULL == supposedType) {
                    // 可能是内联命令
                    SupplierCtx supplierCtx = compileInlineCommand(token);
                    if(supplierCtx != null) {
                        values.add(supplierCtx);
                        stream.next();
                        continue;
                    }
                    throw new IllegalValueException("can not 'LPUSH' value 'null' to a list");
                }
                if(UnmodifiedChecker.isUnmodified(value.getClass())) {
                    // TODO: 2023/11/26 将不可变对象转为 期望的可变对象
//                    supposedType.defaultJavaClass()
                    stream.next();
                    continue;
                }
                // 如果期望的类型是反序列化后返回的类型的父类, 则直接使用
                if(supposedType.defaultJavaClass().isAssignableFrom(value.getClass())) {
                    values.add(value);
                    stream.next();
                    continue;
                }
                // TODO: 2023/11/26 尝试转换



                // 如果序列化的值不满足需求, 则报错
                throw new IllegalValueException("json value: "+token+" is unsupported to store in database");
            } catch (JsonProcessingException e) {
                throw new CommandCompileException("can not parse json(?) '" + token + "' to valid data type." + stream.errToken(token));
            }
        }
    }
}
