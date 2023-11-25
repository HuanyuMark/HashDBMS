package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.exception.CommandCompileException;

/**
 * Date: 2023/11/24 16:44
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class ExistsOptionContext extends BooleanOptionContext {
    public ExistsOptionContext() {
        super(Boolean.TRUE);
    }
    @Override
    public Options key() {
        return Options.EXISTS;
    }
}
