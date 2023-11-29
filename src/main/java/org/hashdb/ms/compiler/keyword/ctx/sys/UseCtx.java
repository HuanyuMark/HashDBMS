package org.hashdb.ms.compiler.keyword.ctx.sys;

import java.util.function.Supplier;

/**
 * Date: 2023/11/30 0:48
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class UseCtx implements SystemCompileCtx<Boolean> {
    @Override
    public Boolean interpret() {


        return Boolean.TRUE;
    }
}
