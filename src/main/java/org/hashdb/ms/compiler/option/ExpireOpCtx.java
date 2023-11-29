package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/24 17:05
 * 默认优先级(低优先级) 删除 配置
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExpireOpCtx extends LongOpCtx {
    public static final Long DEFAULT_EXPIRE_AFTER_MILLISECONDS = -2L;

    /**
     * 默认不过期
     */
    public ExpireOpCtx() {
        super(DEFAULT_EXPIRE_AFTER_MILLISECONDS);
    }
    @Override
    public Options key() {
        return Options.EXPIRE;
    }

    @Override
    protected void beforeCompile(String unknownValueToken, DatabaseCompileStream stream) {
        if(unknownValueToken.isEmpty()) {
            throw new CommandCompileException("expire option require a param(millisecond)."+stream.errToken(""));
        }
    }
}
