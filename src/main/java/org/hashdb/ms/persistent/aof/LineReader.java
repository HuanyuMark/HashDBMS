package org.hashdb.ms.persistent.aof;

import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

/**
 * Date: 2024/2/27 17:57
 *
 * @author Huanyu Mark
 */
public interface LineReader extends Closeable {

    @Nullable
    String readLine() throws IOException;
}
