package org.hashdb.ms.compiler.keyword;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Date: 2023/11/30 13:27
 *
 * @author huanyuMake-pecdle
 */
public interface CompilerNode {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    Keyword<?> name();
}
