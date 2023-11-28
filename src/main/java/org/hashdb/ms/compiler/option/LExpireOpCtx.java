package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/28 22:59
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LExpireOpCtx extends LongOpCtx {
    public LExpireOpCtx() {
        super(ExpireOpCtx.DEFAULT_EXPIRE_AFTER_MILLISECONDS);
    }
    @Override
    public Options key() {
        return Options.LExpire;
    }
    @Override
    protected void beforeCompile(String unknownValueToken, TokenCompileStream stream) {
        if(unknownValueToken.isEmpty()) {
            throw new CommandCompileException("expire option require a param(millisecond)."+stream.errToken(""));
        }
    }
}
