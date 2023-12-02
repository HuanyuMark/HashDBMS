package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.exception.DBClientException;

/**
 * Date: 2023/11/25 18:22
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class IntegerOpCtx extends ParseableOpCtx<Integer> {
    public IntegerOpCtx(Integer defaultValue){
        super(defaultValue);
    }

    @Override
    public IntegerOpCtx compile(String unknownValueToken, DatabaseCompileStream stream) {
        beforeCompile(unknownValueToken, stream);
        try {
            value = Integer.valueOf(unknownValueToken);
        } catch (NumberFormatException e) {
            throw new DBClientException("can not parse string '"+ unknownValueToken +"' to integer. "+stream.errToken(unknownValueToken));
        }
        afterCompile(unknownValueToken, stream);
        return this;
    }

    protected void beforeCompile(String unknownValueToken, DatabaseCompileStream stream){
    }

    protected void afterCompile(String unknownValueToken, DatabaseCompileStream stream){
    }
}
