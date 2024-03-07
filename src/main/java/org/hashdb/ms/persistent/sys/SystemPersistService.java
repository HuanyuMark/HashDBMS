package org.hashdb.ms.persistent.sys;

import org.hashdb.ms.manager.SystemInfo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;

/**
 * Date: 2024/3/5 22:52
 *
 * @author Huanyu Mark
 */
public interface SystemPersistService extends ObjectProvider<SystemInfo> {
    void write(SystemInfo sysInfo) throws IOException;

    SystemInfo read() throws IOException;

    @Override
    default SystemInfo getObject(Object... args) throws BeansException {
        return getObject();
    }

    @Override
    default SystemInfo getIfAvailable() throws BeansException {
        return getObject();
    }

    @Override
    default SystemInfo getIfUnique() throws BeansException {
        return getObject();
    }

    @Override
    default SystemInfo getObject() throws BeansException {
        try {
            return read();
        } catch (IOException e) {
            throw new BeanCreationException("can not read system info file", e);
        }
    }
}
