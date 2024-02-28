package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 17:49
 *
 * @author Huanyu Mark
 */
public abstract class ParseableOpCtx<V> implements OptionCtx<V> {
    protected V value;

    public ParseableOpCtx(V defaultValue) {
        this.value = defaultValue;
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
        return simpleName.substring(0, simpleName.length() - 5) + "=" + value;
    }
}
