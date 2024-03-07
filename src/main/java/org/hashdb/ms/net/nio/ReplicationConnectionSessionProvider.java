package org.hashdb.ms.net.nio;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Date: 2024/3/6 20:45
 *
 * @author Huanyu Mark
 */
@RequiredArgsConstructor
public class ReplicationConnectionSessionProvider implements ObjectProvider<ReplicationConnectionSession> {

    private final AutowireCapableBeanFactory factory;

    @Override
    public @NotNull ReplicationConnectionSession getObject(Object @NotNull ... args) throws BeansException {
        if (args.length < 1) {
            throw new BeanCreationException("no satisfied constructor to create ReplicationConnectionSession");
        }
        if (!(args[0] instanceof BusinessConnectionSession base)) {
            throw new BeanCreationException("no satisfied constructor to create ReplicationConnectionSession");
        }
        var session = new ReplicationConnectionSession(base);
        factory.autowireBean(session);
        return session;
    }

    @Override
    public ReplicationConnectionSession getIfAvailable() throws BeansException {
        throw new NoUniqueBeanDefinitionException(ReplicationConnectionSession.class);
    }

    @Override
    public ReplicationConnectionSession getIfUnique() throws BeansException {
        throw new NoUniqueBeanDefinitionException(ReplicationConnectionSession.class);
    }

    @Override
    public @NotNull ReplicationConnectionSession getObject() throws BeansException {
        throw new NoUniqueBeanDefinitionException(ReplicationConnectionSession.class);
    }
}
