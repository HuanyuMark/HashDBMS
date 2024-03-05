package org.hashdb.ms.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Date: 2024/3/1 10:50
 * 只允许主结点(单一写者)调用被注明的方法, 否则在多读者场景下, 会发生
 * 并发问题
 *
 * @author Huanyu Mark
 */
@Documented
@Target(ElementType.METHOD)
public @interface MasterOnly {
}
