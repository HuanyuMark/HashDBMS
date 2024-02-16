package org.hashdb.ms.net.service;

import org.hashdb.ms.net.msg.MessageType;
import org.hashdb.ms.net.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:37
 *
 * @author huanyuMake-pecdle
 */
public class CandidateMessage extends ServiceMessage {

    private String host;

    private int port;

    @Override
    public MessageType getType() {
        return MessageType.CANDIDATE;
    }
}
