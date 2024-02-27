package org.hashdb.ms.event;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Date: 2023/12/1 1:33
 * 在 {@link org.springframework.context.ApplicationContext} 注入后, 发布该事件
 *
 * @author huanyuMake-pecdle
 */
public record ApplicationContextLoadedEvent(ConfigurableApplicationContext ctx) {
}
