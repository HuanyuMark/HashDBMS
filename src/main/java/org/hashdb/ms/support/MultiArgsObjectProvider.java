package org.hashdb.ms.support;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;

/**
 * Date: 2024/3/7 1:32
 *
 * @author Huanyu Mark
 */
public interface MultiArgsObjectProvider<O> extends ObjectProvider<O> {
    default UnsatisfiedDependencyException report(String methodName) {
        return new UnsatisfiedDependencyException(getClass().getName(), "unknown", "unknown", STR."call getObject(Object... args) instead of call \{methodName}");
    }

    @Override
    default O getIfAvailable() throws BeansException {
        throw report("getIfAvailable()");
    }

    @Override
    default O getIfUnique() throws BeansException {
        throw report("getIfUnique()");
    }

    @Override
    default @NotNull O getObject() throws BeansException {
        throw report("getObject()");
    }
}
