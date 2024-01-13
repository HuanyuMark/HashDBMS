package org.hashdb.ms.data.task;

import org.hashdb.ms.exception.DBClientException;

import java.util.List;
import java.util.Objects;

/**
 * Date: 2023/11/24 14:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class UnmodifiableCollections {
    public static final Class<?> unmodifiableCollection;
    public static final Class<?> unmodifiableList;
    public static final Class<?> unmodifiableAbstractList;
    public static final Class<?> unmodifiableSet;
    public static final Class<?> unmodifiableAbstractSet;

    static {
        try {
            unmodifiableCollection = Class.forName("java.util.Collections$UnmodifiableCollection");
            unmodifiableList = Class.forName("java.util.Collections$UnmodifiableList");
            unmodifiableSet = Class.forName("java.util.Collections$UnmodifiableSet");
            unmodifiableAbstractList = Class.forName("java.util.ImmutableCollections$AbstractImmutableList");
            unmodifiableAbstractSet = Class.forName("java.util.ImmutableCollections$AbstractImmutableSet");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isUnmodifiableList(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return unmodifiableList.isAssignableFrom(clazz) || unmodifiableAbstractList.isAssignableFrom(clazz);
    }

    public static boolean isUnmodifiableSet(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return unmodifiableSet.isAssignableFrom(clazz) || unmodifiableAbstractSet.isAssignableFrom(clazz);
    }

    public static boolean isUnmodifiableCollection(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return unmodifiableCollection.isAssignableFrom(clazz);
    }

    public static boolean isUnmodifiableCollection(List<?> container) {
        Objects.requireNonNull(container);
        return isUnmodifiableCollection(container.getClass());
    }

    public static void check(List<?> container) {
        if (isUnmodifiableCollection(container)) {
            throw new DBClientException(new UnsupportedOperationException("List is not unmodifiable"));
        }
    }
}
