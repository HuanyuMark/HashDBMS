package org.hashdb.ms.net.msg;

import java.util.Date;

/**
 * Date: 2023/12/1 2:11
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface IMessage {
    MessageType getType();

    String getData();

    long getTimestamp();
}
