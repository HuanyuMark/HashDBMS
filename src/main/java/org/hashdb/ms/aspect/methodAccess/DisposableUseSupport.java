package org.hashdb.ms.aspect.methodAccess;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2023/11/21 17:55
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@Aspect
@Component
public class DisposableUseSupport {
    private final Set<Method> usedFlags = ConcurrentHashMap.newKeySet();
    @Before("@annotation(org.hashdb.ms.aspect.methodAccess.DisposableUse) || @within(org.hashdb.ms.aspect.methodAccess.DisposableUse)")
    public void checkSetAction(JoinPoint joinPoint){
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method target = signature.getMethod();
        // 如果已经被调用过，则抛出异常
        if(usedFlags.contains(target)) {
            throw new IllegalCallerException("can`t call method '"+signature+"' more than once");
        } else {
            usedFlags.add(target);
        }
    }
}
