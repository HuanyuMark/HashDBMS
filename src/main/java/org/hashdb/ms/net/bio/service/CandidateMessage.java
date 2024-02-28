package org.hashdb.ms.net.bio.service;

import org.hashdb.ms.net.bio.msg.MessageType;
import org.hashdb.ms.net.bio.msg.ServiceMessage;

/**
 * Date: 2023/12/7 16:37
 *
 * @author Huanyu Mark
 */
public class CandidateMessage extends ServiceMessage {

    private String host;

    private int port;

    @Override
    public MessageType getType() {
        return MessageType.CANDIDATE;
    }
}
