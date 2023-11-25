package org.hashdb.ms.aspect.methodAccess;

import java.lang.annotation.*;

/**
 * Date: 2023/11/22 16:08
 * 只允许 在 Aop 的 BeanPostProcessor 加载前(提供切面)，configuration 加载时，调用被注释的方法
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigLoadOnly {
}
