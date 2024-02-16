package org.hashdb.ms.net.nio.msg.v1;

/**
 * Date: 2024/1/18 21:21
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class AppCommandMessage extends Message<String> {
    public AppCommandMessage(long id, String body) {
        super(id, body);
    }

    public AppCommandMessage(String body) {
        super(body);
    }

    @Override
    public MessageMeta getMeta() {
        return MessageMeta.APP_COMMAND;
    }
}
