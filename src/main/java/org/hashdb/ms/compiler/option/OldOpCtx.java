package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/24 17:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
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
