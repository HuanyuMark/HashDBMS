package org.hashdb.ms.net;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Date: 2024/1/13 21:19
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class Parameter {
    /**
     * 类型为
     * {@link org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx}
     * 或
     * {@link org.hashdb.ms.data.DataType} 里支持的数据类型的java对象实例
     */
    private Object value;

    private final List<Consumer<Object>> updateCbs = new LinkedList<>();

    Parameter(Object value) {
        this.value = value;
    }

    public Object value() {
        return value;
    }

    void notifyUpdate(Object newValue) {
        value = newValue;
        updateCbs.parallelStream().forEach(cb -> cb.accept(newValue));
    }

    public void onUpdate(Consumer<Object> cb) {
        updateCbs.add(cb);
    }
}
