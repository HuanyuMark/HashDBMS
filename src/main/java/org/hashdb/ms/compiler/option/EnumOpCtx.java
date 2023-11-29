package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.exception.IllegalValueException;

import java.util.Arrays;

/**
 * Date: 2023/11/28 21:24
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class EnumOpCtx<E extends Enum<E>> extends ParseableOpCtx<E> implements FlyweightOpCtx {

    public EnumOpCtx(E value) {
        super(value);
    }

    abstract protected Class<? extends Enum<E>> getEnumClass();
    @Override
    public OptionCtx<E> compile(String unknownValueToken, DatabaseCompileStream stream) {
        // 使用默认值
        if(unknownValueToken.isEmpty() && useDefaultValueWhenEmpty()) {
            return this;
        }
        Enum<E>[] enumConstants = getEnumClass().getEnumConstants();
        @SuppressWarnings("unchecked")
        E en = (E) Arrays.stream(enumConstants).filter(e->e.name().equalsIgnoreCase(unknownValueToken))
                .findAny()
                .orElseThrow(()->new IllegalValueException("option value of '"+key()+"' should be one of" + Arrays.toString(enumConstants)));
        value = en;
        afterCompile(en);
        return this;
    }

    protected void afterCompile(E value) {
    }

    protected boolean useDefaultValueWhenEmpty(){
        return true;
    }
}
