package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 17:49
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class ParseableOpCtx<V> implements OptionCtx<V> {
    protected V value;

    public ParseableOpCtx(V value) {
        this.value = value;
    }

    public ParseableOpCtx() {
    }

    @Override
    public V value() {
        return value;
    }

    @Override
    public String toString() {
        String simpleName = getClass().getSimpleName();
        return simpleName.substring(0,simpleName.length()-5)+"="+value;
    }
}
