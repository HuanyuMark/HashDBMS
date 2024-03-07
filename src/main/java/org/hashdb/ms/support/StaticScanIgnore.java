package org.hashdb.ms.support;

import java.lang.annotation.*;

/**
 * Date: 2024/2/28 16:39
 * <p>
 * 不执行静态注入的类, 标注这个
 *
 * @author Huanyu Mark
 * @see StaticAnnotationProcessor
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StaticScanIgnore {
}
