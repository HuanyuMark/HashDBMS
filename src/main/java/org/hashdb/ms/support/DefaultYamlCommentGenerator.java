package org.hashdb.ms.support;

import java.util.List;

/**
 * Date: 2024/2/23 17:18
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class DefaultYamlCommentGenerator implements YamlCommentGenerator {
    @Override
    public List<String> generate(Object bean, String name, Object value, String defaultComment) {
        return List.of(defaultComment);
    }
}
