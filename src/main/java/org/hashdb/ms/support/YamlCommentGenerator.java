package org.hashdb.ms.support;

import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Date: 2024/2/23 17:16
 * 实现类必须有一个无参构造器, 否则无法实例化
 *
 * @author huanyuMake-pecdle
 */
public interface YamlCommentGenerator {
    YamlCommentGenerator DEFAULT = new DefaultYamlCommentGenerator();

    @Nullable
    List<String> generate(Object bean, String name, Object value, String defaultComment);
}
