package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.DBExternalException;

/**
 * Date: 2023/11/25 18:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class IntegerOptionContext extends ParseableOptionContext<Integer> {
    public IntegerOptionContext(Integer defaultValue){
        super(defaultValue);
    }

    @Override
    public IntegerOptionContext compile(String unknownValueToken, TokenCompileStream stream) {
        beforeCompile(unknownValueToken, stream);
        try {
            value = Integer.valueOf(unknownValueToken);
        } catch (NumberFormatException e) {
            throw new DBExternalException("can not parse string '"+ unknownValueToken +"' to integer. "+stream.errToken(unknownValueToken));
        }
        afterCompile(unknownValueToken, stream);
        return this;
    }

    protected void beforeCompile(String unknownValueToken, TokenCompileStream stream){
    }

    protected void afterCompile(String unknownValueToken,TokenCompileStream stream){
    }
}
