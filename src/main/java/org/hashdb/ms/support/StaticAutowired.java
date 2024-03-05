package org.hashdb.ms.support;

import org.springframework.context.ApplicationContext;

import java.lang.annotation.*;

/**
 * Date: 2024/2/28 14:57
 * <p>
 * 对于被标注的静态方法, 静态字段.
 * 前者会从 {@link ApplicationContext}
 * 中获取Bean, 然后被充作调用该静态方法的参数被调用
 * 后者则会通过 {@link ApplicationContext}
 * 直接注入到字段中
 * <p>
 * Note: 对于那些无参的静态方法, 也会被调用一次
 *
 * @author Huanyu Mark
 * @see StaticAutowiredAnnotationProcessor
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface StaticAutowired {
    boolean required() default true;
}
