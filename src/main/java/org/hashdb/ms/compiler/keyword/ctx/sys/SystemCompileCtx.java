package org.hashdb.ms.compiler.keyword.ctx.sys;

import java.util.function.Supplier;

/**
 * Date: 2023/11/30 1:06
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface SystemCompileCtx<R> {
    R interpret();
}
