package org.hashdb.ms.aspect.methodAccess;

import java.lang.annotation.*;

/**
 * Date: 2023/11/21 17:54
 * 被注释的方法只允许使用一次
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Target({ElementType.METHOD,ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DisposableCall {
}
