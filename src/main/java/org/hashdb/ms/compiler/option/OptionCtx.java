package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.DatabaseCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

import java.util.List;

/**
 * Date: 2023/11/24 16:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface OptionCtx<V> {
    List<String> PREFIX = List.of("--", "-");

    Options key();

    V value();

    default OptionCtx<V> prepareCompile(String valueToken, int assignPos, DatabaseCompileStream stream) {
        if(valueToken.isEmpty() && assignPos != -1) {
            throw new CommandCompileException("option '"+key().name().toLowerCase()+"' require value."+stream.errToken(valueToken));
        }
        return compile(valueToken, stream);
    }

    OptionCtx<V> compile(String unknownValueToken, DatabaseCompileStream stream);
}
