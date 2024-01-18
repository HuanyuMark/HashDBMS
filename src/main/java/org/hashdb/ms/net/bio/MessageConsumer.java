package org.hashdb.ms.net.bio;

import org.hashdb.ms.net.msg.Message;

import java.util.function.BiFunction;

/**
 * Date: 2023/12/2 17:36
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public interface MessageConsumer extends BiFunction<Message, MessageConsumerChain, Object> {
}
