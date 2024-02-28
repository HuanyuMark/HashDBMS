package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.util.ReflectCache;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/11/25 3:13
 *
 * @author Huanyu Mark
 */
public interface Keyword<E extends Enum<E>> {
    String name();

    boolean match(@NotNull String unknownToken);

    ReflectCache<? extends CompilerNode> constructor();
}
