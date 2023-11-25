package org.hashdb.ms.compiler.option;

import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.exception.IllegalValueException;

/**
 * Date: 2023/11/24 17:03
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class OldOptionContext extends BooleanOptionContext {

    public OldOptionContext() {
        super(Boolean.TRUE);
    }

    @Override
    public Options key() {
        return Options.OLD;
    }
}
