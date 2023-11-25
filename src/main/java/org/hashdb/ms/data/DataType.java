package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/22 13:38
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
public enum DataType {
    STRING(String.class, List.of("STR", "STRING")),
    //    NUMBER(List.of(Integer.class, Long.class, Double.class, Float.class,
//            BigDecimal.class, BigInteger.class)),
    // 先考虑开发难度问题，再考虑性能
//    NUMBER(List.of(BigDecimal.class, BigInteger.class), List.of("NUM", "NUMBER")),
    NUMBER(List.of(Long.class, Double.class), List.of("NUM", "NUMBER")),
    SET(HashSet.class, List.of("UOSET", "UOS")),
    ORDERED_SET(TreeSet.class, List.of("OSET")),
    MAP(HashMap.class, List.of("MAP")),
    ORDERED_MAP(TreeMap.class, List.of("OMAP")),
    BITMAP(BitSet.class, List.of("BMAP")),
    LIST(List.of(LinkedList.class, ArrayList.class), List.of("LIST", "LS")),
    NULL(List.of(Null.class), List.of());
//    /**
//     * pipe 消费者可以消费这个类型, 其它的都不行
//     */
//    ANY(List.of(), List.of());
    private final Set<Class<?>> javaClasses;

    private final Class<?> defaultJavaClass;
    private final List<String> commandSymbol;
    private static final Map<Class<?>, DataType> javaClassMap = new HashMap<>();
    private static final Map<String, DataType> commandSymbolMap = new HashMap<>();

    private static void registerClass(Class<?> clazz, DataType type) {
        javaClassMap.put(clazz, type);
    }

    private static void registerCommandSymbol(@NotNull List<String> symbols, DataType type) {
        symbols.forEach(symbol -> commandSymbolMap.put(symbol, type));
    }

    DataType(Class<?> clazz, List<String> commandSymbol) {
        this.javaClasses = Set.of(clazz);
        this.commandSymbol = commandSymbol;
        registerClass(clazz, this);
        registerCommandSymbol(commandSymbol, this);
        this.defaultJavaClass = clazz;
    }

    DataType(@NotNull List<Class<?>> classes, List<String> commandSymbol) {
        this.javaClasses = classes.stream()
                .peek(c -> registerClass(c, this))
                .collect(Collectors.toUnmodifiableSet());
        this.commandSymbol = commandSymbol;
        registerCommandSymbol(commandSymbol, this);
        this.defaultJavaClass = classes.getFirst();
    }

    public static @NotNull DataType typeofHValue(@Nullable HValue<?> value) {
        return typeOfRawValue(Objects.requireNonNullElse(value, HValue.EMPTY).data());
    }

    public static @NotNull DataType typeOfRawValue(@Nullable Object instance) throws IllegalJavaClassStoredException {
        Class<?> javaClass = Objects.requireNonNullElse(instance, Null.VALUE).getClass();
        DataType type = javaClassMap.get(javaClass);
        if (type == null) {
            throw IllegalJavaClassStoredException.of(javaClass);
        }
        return type;
    }

    public static DataType typeOfSymbol(String symbol) {
        Objects.requireNonNull(symbol);
        return commandSymbolMap.get(symbol.toUpperCase());
    }

    public Set<Class<?>> javaClasses() {
        return javaClasses;
    }

    public Class<?> defaultJavaClass() {
        return defaultJavaClass;
    }

    public Collection<String> commandSymbols() {
        return commandSymbol;
    }

    @Slf4j
    private static final class Null {
        private Null() {
        }
        public static final Null VALUE=init();

        /**
         * 防止IDEA警告：实例化一个实用类
         */
        private static Null init() {
            try {
                Constructor<Null> constructor = Null.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Throwable e) {
                log.error("unexpected exception: {}",e.toString());
                throw new DBInnerException(e);
            }
        }
    }
}
