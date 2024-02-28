package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;

/**
 * Date: 2023/11/28 22:59
 *
 * @author Huanyu Mark
 */
public class LExpireOpCtx extends LongOpCtx {
    public LExpireOpCtx() {
        super(ExpireOpCtx.DEFAULT_EXPIRE_AFTER_MILLISECONDS);
    }

    @Override
    public Options key() {
        return Options.LEXPIRE;
    }

    @Override
    protected void beforeCompile(String unknownValueToken, DatabaseCompileStream stream) {
        if (unknownValueToken.isEmpty()) {
            throw new CommandCompileException("expire option require a param(millisecond)." + stream.errToken(""));
        }
    }
}
