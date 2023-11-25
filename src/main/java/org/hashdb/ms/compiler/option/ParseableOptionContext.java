package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 17:49
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class ParseableOptionContext<V> implements OptionContext<V> {
    protected V value;

    public ParseableOptionContext(V value) {
        this.value = value;
    }

    public ParseableOptionContext() {
    }
    @Override
    public V value() {
        return value;
    }
}
