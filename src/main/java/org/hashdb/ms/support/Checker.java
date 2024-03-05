package org.hashdb.ms.support;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Date: 2024/2/27 11:58
 *
 * @author Huanyu Mark
 */
@Slf4j
public class Checker {
    public static <N extends Number> int notNegative(N val, N defaultVal, String msg) {
        if (val == null) {
            if (defaultVal == null) {
                throw Exit.error(log, msg, "value is required");
            }
            val = defaultVal;
        }
        return notNegative(val.intValue(), msg);
    }

    public static int notNegative(int val, String msg) {
        if (val < 0) {
            throw Exit.error(log, msg, "value is negative");
        }
        return val;
    }

    public static <N extends Number> @Nullable N notNegativeOrZeroNullable(N val, String msg) {
        if (val == null) {
            return null;
        }
        if (val.intValue() <= 0) {
            throw Exit.error(log, msg, "value should be > 0");
        }
        return val;
    }

    public static <N extends Number> N gt(N val, N compareNum, N defaultVal, String msg) {
        if (val == null) {
            if (defaultVal == null) {
                throw Exit.error(log, msg, "value is required");
            }
            val = defaultVal;
        }
        if (val.longValue() <= compareNum.longValue()) {
            throw Exit.error(log, msg, STR."value should be > \{compareNum}");
        }
        return val;
    }

    public static <N extends Number> N notNegativeOrZero(N val, N defaultVal, String msg) {
        if (val == null) {
            if (defaultVal == null) {
                throw Exit.error(log, msg, "value is required");
            }
            val = defaultVal;
        }
        if (val.intValue() <= 0) {
            throw Exit.error(log, msg, "value should be > 0");
        }
        return val;
    }

    public static int notNegativeOrZero(int val, String msg) {
        if (val <= 0) {
            throw Exit.error(log, msg, "value should be > 0");
        }
        return val;
    }

    public static long notNegativeOrZero(long val, String msg) {
        if (val <= 0) {
            throw Exit.error(log, msg, "value should be > 0");
        }
        return val;
    }

    /**
     * @param values 具有不同优先级的值(由高到低), 最后一个值不能为null
     * @return {@code values} 里第一个非null值
     */
    @SafeVarargs
    public static <T> @NotNull T require(T... values) {
        if (values.length == 0) {
            throw new NoSuchElementException();
        }
        T last = Objects.requireNonNull(values[values.length - 1], "values[values.length - 1] is default value, can not be null");
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        // unreachable
        return last;
    }

    @SafeVarargs
    public static <T> T require(Supplier<T> defaultValue, T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return Objects.requireNonNull(defaultValue.get(), "supplied defaultValue can not be null");
    }

    @SafeVarargs
    public static <T> T require(String msg, String valueName, T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        throw Exit.error(log, msg, STR."\{valueName} is required");
    }

    @SafeVarargs
    public static <T> T require(String msg, String valueName, Supplier<T>... values) {
        for (Supplier<T> value : values) {
            T v = value.get();
            if (v != null) {
                return v;
            }
        }
        throw Exit.error(log, msg, STR."\{valueName} is required");
    }
}
