package org.hashdb.ms.data.task;

import org.hashdb.ms.exception.DBExternalException;

import java.util.List;
import java.util.Objects;

/**
 * Date: 2023/11/24 14:51
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public class UnmodifiedChecker {
    public static final Class<?> unmodifiableCollection;
    public static final Class<?> unmodifiableList;
    static {
        try {
            unmodifiableCollection = Class.forName("java.util.Collections$UnmodifiableCollection");
            unmodifiableList = Class.forName("java.util.Collections$UnmodifiableList");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
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
            throw new DBExternalException(new UnsupportedOperationException("List is not unmodifiable"));
        }
    }
}
