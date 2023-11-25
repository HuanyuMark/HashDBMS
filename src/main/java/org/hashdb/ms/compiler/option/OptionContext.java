package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.TokenCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

import java.util.List;

/**
 * Date: 2023/11/24 16:41
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface OptionContext<V> {
    List<String> PREFIX = List.of("--", "-");

    Options key();

    V value();

    default OptionContext<V> prepareCompile(String valueToken, int assignPos, TokenCompileStream stream) {
        if(valueToken.isEmpty() && assignPos != -1) {
            throw new CommandCompileException("the value of option '"+key().name().toLowerCase()+"' should not be empty."+stream.errToken());
        }
        return compile(valueToken, stream);
    }

    OptionContext<V> compile(String unknownValueToken, TokenCompileStream stream);
}
