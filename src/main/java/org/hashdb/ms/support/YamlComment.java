package org.hashdb.ms.support;

import java.lang.annotation.*;

/**
 * Date: 2024/2/23 17:15
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface YamlComment {
    String value() default "";

    Class<? extends YamlCommentGenerator> generator() default DefaultYamlCommentGenerator.class;

    /**
     * generator 是否单例. 如果不是, 则每次解析字段, 都会重新创建一个 generator 实例
     */
    boolean singleton() default true;
}
