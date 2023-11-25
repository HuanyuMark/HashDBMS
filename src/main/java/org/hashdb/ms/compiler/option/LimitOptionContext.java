package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.IllegalValueException;

/**
 * Date: 2023/11/24 16:46
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class LimitOptionContext extends IntegerOptionContext {

    public LimitOptionContext() {
        super(null);
    }

    @Override
    public Options key() {
        return Options.LIMIT;
    }

    @Override
    protected void afterCompile(String unknownValueToken, TokenCompileStream stream) {
        if(value < 0) {
            throw new IllegalValueException("pop option should be greater then 0");
        }
    }
}
