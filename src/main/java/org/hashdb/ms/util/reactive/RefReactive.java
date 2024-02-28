package org.hashdb.ms.util.reactive;

import java.lang.reflect.ParameterizedType;
import java.util.function.BiConsumer;

/**
 * Date: 2024/2/21 19:07
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
public interface RefReactive<O> extends Reactive {

    int DEBOUNCE_INTERVAL = 100;

    /**
     * @param value 新值
     * @return 旧值
     */
    O set(O value);

    O get();

    /**
     * @param listener 监听器回调, 第一个参数是新值,第二个参数是旧值
     */
    void onChange(BiConsumer<O, O> listener);

    @Override
    Class<? extends O> getTargetClass();

    /**
     * @return 引用的值的字符串表达式
     */
    @Override
    String toString();

    static <O> RefReactive<O> of(O value) {
        return new SimpleRefReactive<>(value);
    }

    static <O> RefReactive<O> ofConcurrent(O value) {
        return new ConcurrentRefReactive<>(value);
    }

    abstract class AbstractRefReactive<O> implements RefReactive<O> {
        protected Class<? extends O> targetClass;

        public String toString() {
            return String.valueOf(get());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends O> getTargetClass() {
            if (targetClass != null) {
                return targetClass;
            }
            var genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
            targetClass = ((Class<? extends O>) genericSuperclass.getActualTypeArguments()[0]);
            return targetClass;
        }
    }

}
