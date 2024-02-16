package org.hashdb.ms.aspect.methodAccess;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Date: 2023/11/22 16:10
 *
 * @author huanyuMake-pecdle
 */
@Aspect
@Component
public class ConfigLoadOnlySupport {
    @Before("@annotation(ConfigLoadOnly) || @within(ConfigLoadOnly)")
    public void checkConfigLoadOnly(JoinPoint joinPoint) {
        Method target = ((MethodSignature) joinPoint.getSignature()).getMethod();
        throw new UnsupportedOperationException("can`t call method '" + target + "' after configurations have been loaded");
    }
}
