package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/27 23:14
 *
 * @author Huanyu Mark
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
