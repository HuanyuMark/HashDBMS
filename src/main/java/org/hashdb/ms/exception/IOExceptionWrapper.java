package org.hashdb.ms.exception;

import java.io.IOException;

/**
 * Date: 2024/3/3 16:49
 *
 * @author Huanyu Mark
 */
public class IOExceptionWrapper extends RuntimeException {
    public IOExceptionWrapper(IOException cause) {
        super(cause);
    }

    @Override
    public synchronized IOException getCause() {
        return ((IOException) super.getCause());
    }
}
