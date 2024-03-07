package org.hashdb.ms.persistent.aof;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Date: 2024/2/28 13:09
 *
 * @author Huanyu Mark
 */
@Slf4j
public class EachAofFlusher extends AbstractAofFlusher {

    public EachAofFlusher(Aof file) throws IOException {
        super(file);
    }
}
