package org.hashdb.ms.aspect.methodAccess;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Date: 2023/11/21 17:55
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@Aspect
@Component
public class DisposableCallSupport {
    private final Map<Method, Boolean> usedFlags = new ConcurrentHashMap<>();

    @Before("@annotation(org.hashdb.ms.aspect.methodAccess.DisposableCall) || @within(org.hashdb.ms.aspect.methodAccess.DisposableCall)")
    public void checkSetAction(JoinPoint joinPoint) {
        var signature = (MethodSignature) joinPoint.getSignature();
        var target = signature.getMethod();
        // 如果已经被调用过，则抛出异常
        if (usedFlags.put(target, Boolean.TRUE) != null) {
            throwCallException(signature);
        }
    }

    private void throwCallException(MethodSignature signature) {
        throw new IllegalCallerException("can`t call method '" + signature + "' more than once");
    }
}
