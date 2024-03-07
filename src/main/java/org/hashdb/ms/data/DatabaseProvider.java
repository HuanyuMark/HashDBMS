package org.hashdb.ms.data;

import lombok.RequiredArgsConstructor;
import org.hashdb.ms.support.MultiArgsObjectProvider;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Date: 2024/3/6 18:46
 *
 * @author Huanyu Mark
 */
@Component
@RequiredArgsConstructor
public class DatabaseProvider implements MultiArgsObjectProvider<Database> {
    private final AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Override
    public @NotNull Database getObject(Object @NotNull ... args) throws BeansException {
        if (args.length == 0) {
            throw new IllegalArgumentException("require args.length > 0");
        }

        if (args[0] instanceof DatabaseInfos infos) {
            var db = new Database(infos);
            autowireCapableBeanFactory.autowireBean(db);
            return db;
        }

        var parameterTypes = Arrays.stream(args).map(a -> a == null ? "null" : a.getClass().getSimpleName()).toArray();
        throw new BeanCreationException(STR."can not match construct\{Arrays.toString(parameterTypes)}");
    }
}
