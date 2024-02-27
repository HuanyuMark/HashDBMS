package org.hashdb.ms.event;

import org.hashdb.ms.config.ClusterGroupConfig;

/**
 * Date: 2023/12/5 16:41
 *
 * @author huanyuMake-pecdle
 */
public record ReplicationConfigLoadedEvent(ClusterGroupConfig config) {
}
