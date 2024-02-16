package org.hashdb.ms.net.client;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hashdb.ms.net.service.ReplicationHeartbeatMessage;

/**
 * Date: 2023/12/7 16:18
 *
 * @author huanyuMake-pecdle
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class ActReplHeartbeatMessage extends ReplicationHeartbeatMessage {
}
