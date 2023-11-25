package org.hashdb.ms.data.task;

import org.hashdb.ms.exception.DBExternalException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Date: 2023/11/24 14:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class UnmodifiedChecker {
    public static final Class<?> unmodifiableCollectionClass;
    public static final Class<?> unmodifiableListClass;
    static {
        try {
            unmodifiableCollectionClass = Class.forName("java.util.Collections$UnmodifiableCollection");
            unmodifiableListClass = Class.forName("java.util.Collections$UnmodifiableList");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isUnmodified(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return unmodifiableCollectionClass.isAssignableFrom(clazz);
    }
    public static boolean isUnmodified(List<?> container) {
        Objects.requireNonNull(container);
        return isUnmodified(container.getClass());
    }

    public static void check(List<?> container) {
        if (isUnmodified(container)) {
            throw new DBExternalException(new UnsupportedOperationException("List is not unmodifiable"));
        }
    }
}
