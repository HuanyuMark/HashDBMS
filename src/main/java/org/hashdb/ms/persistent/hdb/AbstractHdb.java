package org.hashdb.ms.persistent.hdb;

import lombok.Getter;
import org.hashdb.ms.config.HdbConfig;
import org.hashdb.ms.support.StaticAutowired;

/**
 * Date: 2024/3/3 13:07
 *
 * @author Huanyu Mark
 */
public abstract class AbstractHdb {
    @Getter
    @StaticAutowired
    private static HdbConfig hdbConfig;

    protected AbstractHdb() {
    }
}
