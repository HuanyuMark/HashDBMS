package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 16:44
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExistsOpCtx extends BooleanOpCtx {
    public ExistsOpCtx() {
        super(Boolean.TRUE);
    }
    @Override
    public Options key() {
        return Options.EXISTS;
    }
}
