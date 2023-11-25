package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.config.DBRamConfig;
import org.hashdb.ms.exception.DBInnerException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.util.Lazy;
import org.hashdb.ms.util.OneTimeLazy;
import org.hashdb.ms.util.ReflectCacheData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
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

    private final ReflectCacheData<?> reflectCacheData;
    private final List<String> commandSymbol;
    private static Map<Class<?>, DataType> javaClassMap;
    private static Map<String, DataType> commandSymbolMap;

    private static final OneTimeLazy<?> loadJavaClassOfMapDataTypeFromConfig = OneTimeLazy.of(()->{
        DBRamConfig dbRamConfig = HashDBMSApp.ctx().getBean(DBRamConfig.class);
        if (dbRamConfig.isStoreLikeJsonSequence()) {
            javaClassMap.remove(HashMap.class);
            javaClassMap.put(LinkedHashMap.class,MAP);
        }
        return null;
    });
    private static void registerClass(Class<?> clazz, DataType type) {
        if(javaClassMap == null) {
            javaClassMap = new HashMap<>();
        }
        javaClassMap.put(clazz, type);
    }

    private static void registerCommandSymbol(@NotNull List<String> symbols, DataType type) {
        if(commandSymbolMap == null) {
            commandSymbolMap = new HashMap<>();
        }
        symbols.forEach(symbol -> commandSymbolMap.put(symbol, type));
    }

    DataType(Class<?> clazz, List<String> commandSymbol) {
        this.javaClasses = Set.of(clazz);
        this.commandSymbol = commandSymbol;
        registerClass(clazz, this);
        registerCommandSymbol(commandSymbol, this);
        reflectCacheData = new ReflectCacheData<>(clazz);
    }

    DataType(@NotNull List<Class<?>> classes, List<String> commandSymbol) {
        this.javaClasses = classes.stream()
                .peek(c -> registerClass(c, this))
                .collect(Collectors.toUnmodifiableSet());
        this.commandSymbol = commandSymbol;
        registerCommandSymbol(commandSymbol, this);
        reflectCacheData = new ReflectCacheData<>(classes.getFirst());
    }

    public static @NotNull DataType typeofHValue(@Nullable HValue<?> value) {
        return typeOfRawValue(Objects.requireNonNullElse(value, HValue.EMPTY).data());
    }

    public static @NotNull DataType typeOfRawValue(@Nullable Object instance) throws IllegalJavaClassStoredException {
        loadJavaClassOfMapDataTypeFromConfig.get();
        Class<?> javaClass = Objects.requireNonNullElse(instance, Null.VALUE).getClass();
        DataType type = javaClassMap.get(javaClass);
        if (type == null) {
            throw IllegalJavaClassStoredException.of(javaClass);
        }
        return type;
    }

    @Nullable
    public static DataType typeOfSymbol(String symbol) {
        Objects.requireNonNull(symbol);
        return commandSymbolMap.get(symbol.toUpperCase());
    }

    public Set<Class<?>> javaClasses() {
        return javaClasses;
    }

    public ReflectCacheData<?> reflect() {
        return reflectCacheData;
    }

    public Collection<String> commandSymbols() {
        return commandSymbol;
    }

    public static boolean canStore(Class<?> clazz) {
        return javaClassMap.containsKey(clazz);
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
