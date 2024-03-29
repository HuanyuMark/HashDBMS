package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;

/**
 * Date: 2023/11/26 20:29
 * 高优先级删除 配置
 *
 * @author Huanyu Mark
 */
public class HExpireOpCtx extends LongOpCtx {
    public static final Long DEFAULT_VALUE = ExpireOpCtx.DEFAULT_EXPIRE_AFTER_MILLISECONDS;

    public HExpireOpCtx() {
        super(DEFAULT_VALUE);
    }

    @Override
    public Options key() {
        return Options.HEXPIRE;
    }

    @Override
    protected void beforeCompile(String unknownValueToken, DatabaseCompileStream stream) {
        if (unknownValueToken.isEmpty()) {
            throw new CommandCompileException("expire option require a param(millisecond)." + stream.errToken(""));
        }
    }
}
