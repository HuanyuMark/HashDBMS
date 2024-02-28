package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.compiler.exception.CommandCompileException;

/**
 * Date: 2023/11/25 18:18
 *
 * @author Huanyu Mark
 */
public abstract class LongOpCtx extends ParseableOpCtx<Long> {
    public LongOpCtx(Long defaultValue) {
        super(defaultValue);
    }

    @Override
    public LongOpCtx compile(String unknownValueToken, DatabaseCompileStream stream) {
        beforeCompile(unknownValueToken, stream);
        try {
            value = Long.parseLong(unknownValueToken);
        } catch (NumberFormatException e) {
            throw new CommandCompileException("can not parse string '" + unknownValueToken + "' to integer." + stream.errToken(unknownValueToken));
        }
        afterCompile(unknownValueToken, stream);
        return this;
    }

    protected void beforeCompile(String unknownValueToken, DatabaseCompileStream stream) {
    }

    protected void afterCompile(String unknownValueToken, DatabaseCompileStream stream) {
    }
}
