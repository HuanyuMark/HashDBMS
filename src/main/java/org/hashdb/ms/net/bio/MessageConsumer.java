package org.hashdb.ms.net.bio;

import org.hashdb.ms.net.bio.msg.Message;

import java.util.function.BiFunction;

/**
 * Date: 2023/12/2 17:36
 *
 * @author Huanyu Mark
 */
@Deprecated
public interface MessageConsumer extends BiFunction<Message, MessageConsumerChain, Object> {
}
