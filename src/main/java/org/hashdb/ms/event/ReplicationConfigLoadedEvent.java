package org.hashdb.ms.event;

import org.hashdb.ms.net.nio.ClusterGroup;

/**
 * Date: 2023/12/5 16:41
 *
 * @author Huanyu Mark
 */
public record ReplicationConfigLoadedEvent(ClusterGroup config) {
}
