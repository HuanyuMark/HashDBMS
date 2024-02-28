package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 17:03
 *
 * @author Huanyu Mark
 */
public class OldOpCtx extends BooleanOpCtx {

    public OldOpCtx() {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return Options.OLD;
    }
}
