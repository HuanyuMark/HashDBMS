package org.hashdb.ms.net;

import lombok.Data;

import java.util.UUID;

/**
 * Date: 2023/12/5 13:47
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Data
public class ServerNode {
    private UUID id = UUID.randomUUID();
    private String host = "127.0.0.1";
    private int port;
}
