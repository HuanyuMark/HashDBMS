package org.hashdb.ms.event;

import org.hashdb.ms.config.ReplicationConfig;

/**
 * Date: 2023/12/5 16:41
 *
 * @author huanyuMake-pecdle
 */
public record ReplicationConfigLoadedEvent(ReplicationConfig config) {
}
