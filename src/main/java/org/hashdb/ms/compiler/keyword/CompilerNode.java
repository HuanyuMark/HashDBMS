package org.hashdb.ms.compiler.keyword;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Date: 2023/11/30 13:27
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface CompilerNode {
    @JsonProperty
    Keyword<?> name();

    /**
     * 这个方法仅仅只是用来占位的, 并不会在反序列化的时候被调用
     * 因为 {@link #name()} 的返回值就是只读的, 不需要通过 setter 来
     * 修改 name 的值, 这个方法仅仅是用来标识, 序列化时需要包含 name
     * 这个字段
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    default void name(String name) {
        throw new UnsupportedOperationException();
    }
}
