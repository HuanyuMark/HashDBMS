package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/27 23:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DestructOpCtx extends BooleanOpCtx {
    public DestructOpCtx() {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return null;
    }
}
