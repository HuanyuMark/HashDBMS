package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.consumer.OpsConsumerTask;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.DestructOpCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.task.ImmutableChecker;
import org.hashdb.ms.exception.CommandCompileException;
import org.hashdb.ms.exception.CommandExecuteException;
import org.hashdb.ms.exception.IllegalValueException;
import org.hashdb.ms.exception.StopComplieException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Date: 2023/11/26 19:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class PushCtx extends MutateListCtx {
    protected final List<Object> values = new LinkedList<>();

    protected PushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected OpsConsumerTask<List<?>, ?> compile() throws StopComplieException {
        doCompile();
        return (returnList) -> {
            @SuppressWarnings("unchecked")
            List<Object> opsTarget = ((List<Object>) returnList.getFirst());
            return ()-> {
                for (Object value : beforePush()) {
                    // 运行内联命令
                    if (!(value instanceof SupplierCtx supplierCtx)) {
                        doPushRaw(opsTarget,value);
                        continue;
                    }
                    Object o = getSuppliedValue(supplierCtx);
                    // 如果这个内联命令返回的是一个不存在于数据库中的值 (Immutable), 则全部加入
                    if (ImmutableChecker.unmodifiableList.isAssignableFrom(o.getClass())
                            || ImmutableChecker.unmodifiableSet.isAssignableFrom(o.getClass())) {
                        @SuppressWarnings("unchecked")
                        Collection<Object> co = (Collection<Object>) o;
                        checkNullInCollection(co);
                        doPushCollection(opsTarget, co);
                        continue;
                    }
                    // 如果返回的是list 并且配置了解构, 就将这个链表展开, 加入到操作对象中
                    DestructOpCtx destructOpCtx = supplierCtx.getOption(DestructOpCtx.class);
                    boolean destruct = destructOpCtx != null && destructOpCtx.value();
                    if (List.class.isAssignableFrom(o.getClass()) || Set.class.isAssignableFrom(o.getClass()) && destruct) {
                        @SuppressWarnings("unchecked")
                        Collection<Object> co = (Collection<Object>) o;
                        checkNullInCollection(co);
                        doPushCollection(opsTarget,co);
                        continue;
                    }
                    if (ImmutableChecker.isUnmodifiableCollection(o.getClass())) {
                        throw new CommandExecuteException("keyword '" + name() + "' can not receive data type '" +
                                List.of(DataType.MAP, DataType.ORDERED_MAP, DataType.BITMAP) + "'. " + stream.errToken(supplierCtx.command()));
                    }
                }
                return opsTarget.size();
            };
        };
    }

    private void doCompile(){
        while (true) {
            try {
                if(compilePipe()) {
                    return;
                }
                filterAllOptions();
                try {
                    compileJsonValues((dataType,value)->{
                        values.add(value);
                        return true;
                    });
                } catch (CommandCompileException e) {
                    if (compilePipe()) {
                        break;
                    }
                    throw e;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                break;
            }
        }
        if(values.isEmpty()) {
            throw new CommandCompileException("No value to push");
        }
    }

    @Override
    protected void beforeCompilePipe() {
        if(values.size() > 1) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one value to push");
        }
    }

    abstract protected void doPushRaw(List<Object> opsTarget, Object rawValue);

    abstract protected void doPushCollection(List<Object> opsTarget, Collection<Object> other);

    protected List<Object> beforePush(){
        return values;
    }
    @Override
    public Class<?> supplyType() {
        return Integer.class;
    }

    protected void checkNullInCollection(Collection<?> collection) {
        for (Object o : collection) {
            if(o instanceof Collection<?> c) {
                checkNullInCollection(c);
            }
            if (o == null) {
                throw new IllegalValueException("keyword '" + name() + "' can not consume null value");
            }
        }
    }
}
