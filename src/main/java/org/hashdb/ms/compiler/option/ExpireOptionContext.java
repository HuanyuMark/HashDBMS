package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/24 17:05
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExpireOptionContext extends LongOptionContext {
    public static final ExpireOptionContext DEFAULT = new ExpireOptionContext();
    public static final Long DEFAULT_EXPIRE_AFTER_MILLISECONDS = -2L;

    /**
     * 默认不过期
     */
    private ExpireOptionContext() {
        super(DEFAULT_EXPIRE_AFTER_MILLISECONDS);
    }
    @Override
    public Options key() {
        return Options.EXPIRE;
    }

    @Override
    protected void beforeCompile(String unknownValueToken, TokenCompileStream stream) {
        if(unknownValueToken.isEmpty()) {
            throw new CommandCompileException("expire option require a param(millisecond)."+stream.errToken(""));
        }
    }
}
