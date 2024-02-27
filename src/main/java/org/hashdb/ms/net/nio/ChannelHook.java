package org.hashdb.ms.net.nio;

import io.netty.channel.Channel;

/**
 * Date: 2024/2/1 17:05
 *
 * @author huanyuMake-pecdle
 */
public interface ChannelHook {
    default void onChannelChange(Channel channel) {
    }
}
