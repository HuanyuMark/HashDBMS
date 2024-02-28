package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.exception.IllegalValueException;

import java.util.Arrays;

/**
 * Date: 2023/11/28 21:24
 *
 * @author Huanyu Mark
 */
public abstract class EnumOpCtx<E extends Enum<E>> extends ParseableOpCtx<E> implements FlyweightOpCtx {

    public EnumOpCtx(E defaultValue) {
        super(defaultValue);
    }

    abstract protected Class<? extends Enum<E>> getEnumClass();

    @Override
    @SuppressWarnings("unchecked")
    public OptionCtx<E> compile(String unknownValueToken, DatabaseCompileStream stream) {
        // 使用默认值
        if (unknownValueToken.isEmpty() && useDefaultValueWhenEmpty()) {
            return this;
        }
        E matchedEnumInstance = null;
        E[] enumConstants = (E[]) getEnumClass().getEnumConstants();
        for (E enumInstance : enumConstants) {
            if (enumInstance.name().equalsIgnoreCase(unknownValueToken)) {
                matchedEnumInstance = enumInstance;
                break;
            }
        }
        if (matchedEnumInstance == null) {
            throw new IllegalValueException("option value of '" + key() + "' should be one of" + Arrays.toString(enumConstants));
        }
        value = matchedEnumInstance;
        afterCompile(matchedEnumInstance);
        return this;
    }

    protected void afterCompile(E value) {
    }

    protected boolean useDefaultValueWhenEmpty() {
        return true;
    }
}
