package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/17 16:21
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class CloseMessage extends Message<String> {

    public CloseMessage(long id, String body) {
        super(id, body);
    }

    /**
     * @param body 原因
     */
    public CloseMessage(String body) {
        super(body);
    }

    @Override
    public MessageType type() {
        return null;
    }
}
