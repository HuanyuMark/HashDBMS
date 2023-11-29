package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/29 9:52
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LDeleteOpCtx extends BooleanOpCtx {
    public LDeleteOpCtx(Boolean defaultValue) {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return Options.DELETE;
    }
}
