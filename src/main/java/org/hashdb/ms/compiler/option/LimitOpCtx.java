package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.exception.IllegalValueException;

/**
 * Date: 2023/11/24 16:46
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LimitOpCtx extends LongOpCtx {

    public LimitOpCtx() {
        super(null);
    }

    @Override
    public Options key() {
        return Options.LIMIT;
    }

    @Override
    protected void afterCompile(String unknownValueToken, DatabaseCompileStream stream) {
        if(value < 0) {
            throw new IllegalValueException("option value of '"+key()+"' should be greater then 0");
        }
    }
}
