package org.hashdb.ms.persistent.hdb;

import org.hashdb.ms.data.HValue;

import java.io.Closeable;
import java.util.function.Consumer;

/**
 * Date: 2024/3/5 16:50
 *
 * @author Huanyu Mark
 */
public interface HdbReader extends Closeable {
    void read(Consumer<HValue<?>> acceptor);
}
