package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.data.HValue;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

/**
 * Date: 2024/3/5 16:50
 *
 * @author Huanyu Mark
 */
public interface HdbWriter extends Closeable {
    void write(Collection<HValue<?>> source) throws IOException;
}
