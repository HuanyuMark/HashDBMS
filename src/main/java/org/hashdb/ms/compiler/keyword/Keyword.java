package org.hashdb.ms.compiler.keyword;

import org.hashdb.ms.compiler.keyword.ctx.CmdCtx;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;

/**
 * Date: 2023/11/25 3:13
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface Keyword {

    String name();

    boolean match(@NotNull String unknownToken);

    ReflectCacheData<? extends CmdCtx> cmdCtxFactory();
}
