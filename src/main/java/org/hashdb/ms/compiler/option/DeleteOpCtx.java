package org.hashdb.ms.compiler.option;

/**
 * Date: 2023/11/29 9:52
 *
 * @author huanyuMake-pecdle
 */
public class DeleteOpCtx extends BooleanOpCtx {
    public DeleteOpCtx(Boolean defaultValue) {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return Options.DELETE;
    }
}
