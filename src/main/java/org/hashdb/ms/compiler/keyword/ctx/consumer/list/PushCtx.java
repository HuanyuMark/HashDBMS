package org.hashdb.ms.compiler.keyword.ctx.consumer.list;

import org.hashdb.ms.compiler.ConsumerCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;
import org.hashdb.ms.compiler.exception.CommandExecuteException;
import org.hashdb.ms.compiler.exception.IllegalValueException;
import org.hashdb.ms.compiler.keyword.ctx.CompileCtx;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.compiler.option.DestructOpCtx;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Date: 2023/11/26 19:22
 *
 * @author huanyuMake-pecdle
 */
public abstract class PushCtx extends MutableListCtx {
    @Override
    public void setStream(ConsumerCompileStream stream) {
        super.setStream(stream);
        stream.toWrite();
    }

    protected final List<Object> values = new LinkedList<>();

    protected PushCtx(CompileCtx<?> fatherCompileCtx) {
        super(fatherCompileCtx);
    }

    @Override
    protected void beforeCompile() {
        doCompile();
        beforeCompilePipe();
    }

    @Override
    protected Object operateWithMutableList(List<Object> opsTarget) {
        for (Object value : beforePush()) {
            // 运行内联命令
            if (!(value instanceof SupplierCtx supplierCtx)) {
                doPushRaw(opsTarget, value);
                continue;
            }
            Object o = exeSupplierCtx(supplierCtx);
            // 如果这个内联命令返回的是一个不存在于数据库中的值 (Immutable), 则全部加入
            if (UnmodifiableCollections.unmodifiableList.isAssignableFrom(o.getClass())
                    || UnmodifiableCollections.unmodifiableSet.isAssignableFrom(o.getClass())) {
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
                doPushCollection(opsTarget, co);
                continue;
            }
            if (UnmodifiableCollections.isUnmodifiableCollection(o.getClass())) {
                throw new CommandExecuteException("keyword '" + name() + "' can not receive data type '" +
                        List.of(DataType.MAP, DataType.ORDERED_MAP, DataType.BITMAP) + "'. " + stream().errToken(supplierCtx.command()));
            }
        }
        return opsTarget.size();
    }

    @Override
    protected Object operateWithHValue(HValue<List<Object>> opsValue) {
        return operateWithMutableList(opsValue.data());
    }

    private void doCompile() {
        while (true) {
            try {
                if (compilePipe()) {
                    return;
                }
                filterAllOptions();
                try {
                    compileJsonValues((dataType, value) -> {
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
    }

    @Override
    protected void beforeCompilePipe() {
        if (values.isEmpty()) {
            throw new CommandCompileException("keyword '" + name() + "' require at lease one value to push");
        }
    }

    abstract protected void doPushRaw(List<Object> opsTarget, Object rawValue);

    abstract protected void doPushCollection(List<Object> opsTarget, Collection<Object> other);

    protected List<Object> beforePush() {
        return values;
    }

    @Override
    public @NotNull Class<?> supplyType() {
        return Integer.class;
    }

    protected void checkNullInCollection(Collection<?> collection) {
        for (Object o : collection) {
            if (o instanceof Collection<?> c) {
                checkNullInCollection(c);
            }
            if (o == null) {
                throw new IllegalValueException("keyword '" + name() + "' can not consume null value");
            }
        }
    }
}
