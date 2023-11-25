package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/25 18:14
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public abstract class BooleanOptionContext extends ParseableOptionContext<Boolean> {
    public BooleanOptionContext(Boolean defaultValue){
        super(defaultValue);
    }
    @Override
    public BooleanOptionContext compile(String unknownValueToken, TokenCompileStream stream) {
        // 使用默认值
        if(useDefaultValueWhenEmpty() && unknownValueToken.isEmpty()) {
            return this;
        }
        if("t".equalsIgnoreCase(unknownValueToken) || "true".equalsIgnoreCase(unknownValueToken)) {
            value = true;
            return this;
        }
        if("f".equalsIgnoreCase(unknownValueToken) || "false".equalsIgnoreCase(unknownValueToken)) {
            value = false;
            return this;
        }
        throw new CommandCompileException("can not parse string '"+unknownValueToken+"' to boolean. "+stream.errToken(unknownValueToken));
    }

    boolean useDefaultValueWhenEmpty(){
        return true;
    }
}
