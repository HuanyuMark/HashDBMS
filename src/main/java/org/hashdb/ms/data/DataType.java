package org.hashdb.ms.data;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.exception.DBSystemException;
import org.hashdb.ms.exception.IllegalJavaClassStoredException;
import org.hashdb.ms.util.ReflectCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Date: 2023/11/22 13:38
 *
 * @author huanyuMake-pecdle
 */
public enum DataType {
    STRING(String.class, List.of("STR", "STRING"), String.class),
    // TODO: 2024/1/12 支持 BigDecimal.class, BigInteger.class
    NUMBER(List.of(Long.class, Double.class), List.of("NUM", "NUMBER"), Number.class),
    SET(
            HashSet.class,
            List.of("UOSET", "UOS"),
            Collection.class,
            s -> ((Collection<?>) s).parallelStream()
                    .map(v -> DataType.typeOfRawValue(v).cloner.apply(v))
                    .collect(Collectors.toSet())
    ),
    ORDERED_SET(
            TreeSet.class,
            List.of("OSET"),
            Collection.class,
            s -> new TreeSet<>((((Collection<?>) s))
                    .parallelStream().map(v -> DataType.typeOfRawValue(v).cloner.apply(v))
                    .toList())
    ),
    MAP(
            HashMap.class,
            List.of("MAP"),
            Map.class,
            s -> (((HashMap<?, ?>) s)).entrySet()
                    .parallelStream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), DataType.typeOfRawValue(entry.getValue()).cloner.apply(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
    ),
    ORDERED_MAP(
            TreeMap.class,
            List.of("OMAP"),
            Map.class,
            s -> new TreeMap<>(((TreeMap<?, ?>) s).entrySet().parallelStream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), DataType.typeOfRawValue(entry.getValue()).cloner.apply(entry.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
    ),
    BITMAP(BitSet.class, List.of("BMAP"), BitSet.class, s -> ((BitSet) s).clone()),
    LIST(
            List.of(LinkedList.class, ArrayList.class),
            List.of("LIST", "LS"),
            List.class,
            s -> ((List<?>) s).parallelStream()
                    .map(v -> DataType.typeOfRawValue(v).cloner.apply(v))
                    .collect(Collectors.toList())
    ),
    NULL(List.of(Null.class), List.of(), Null.class, s -> {
        throw new UnsupportedOperationException("can not clone null");
    });

    private final Set<Class<?>> javaClasses;
    private final Function<Object, Object> cloner;
    private final ReflectCache<?> reflectCache;
    private final List<String> typeSymbol;

    private final Class<?> clonableClass;

    private static Map<Class<?>, Set<DataType>> clonableClassMap;
    private static Map<Class<?>, DataType> javaClassMap;
    private static Map<String, DataType> commandSymbolMap;


    static {
        // 配合配置项: dbRamConfig.isStoreLikeJsonSequence()
        javaClassMap.put(LinkedHashMap.class, MAP);
        clonableClassMap.replaceAll((clonableClass, set) -> Collections.unmodifiableSet(set));
    }

    public static class DataTypeInstanceFactory<T> extends ReflectCache<T> {
        public DataTypeInstanceFactory(Class<? extends T> clazz) {
            super(clazz);
        }

        public DataTypeInstanceFactory(Class<? extends T> clazz, Function<Class<?>, Constructor<?>> constructorFinder) {
            super(clazz, constructorFinder);
        }
    }

    private static void registerClass(Class<?> clazz, DataType type) {
        if (javaClassMap == null) {
            javaClassMap = new HashMap<>();
        }
        javaClassMap.put(clazz, type);
    }

    private static void registerClonableClass(Class<?> clazz, DataType type) {
        if (clonableClassMap == null) {
            clonableClassMap = new LinkedHashMap<>();
        }
        Set<DataType> thisTypeSet = clonableClassMap.computeIfAbsent(clazz, k -> new HashSet<>());
        thisTypeSet.add(type);
    }

    private static void registerCommandSymbol(@NotNull List<String> symbols, DataType type) {
        if (commandSymbolMap == null) {
            commandSymbolMap = new HashMap<>();
        }
        symbols.forEach(symbol -> commandSymbolMap.put(symbol, type));
    }

    DataType(Class<?> clazz, List<String> typeSymbol, Class<?> clonableClass, Function<Object, Object> cloner) {
        this(List.of(clazz), typeSymbol, clonableClass, cloner);
    }

    DataType(@NotNull List<Class<?>> classes, List<String> typeSymbol, Class<?> clonableClass, Function<Object, Object> cloner) {
        this.javaClasses = classes.stream()
                .peek(c -> registerClass(c, this))
                .collect(Collectors.toUnmodifiableSet());
        this.typeSymbol = typeSymbol;
        this.clonableClass = clonableClass;
        registerClonableClass(clonableClass, this);
        registerCommandSymbol(typeSymbol, this);
        // TODO: 2024/1/12 这里可能有bug, 比如说数字型那里, 要具体情况具体分析, 不存在可以通用第一种class来实例化的情况
        reflectCache = new ReflectCache<>(classes.getFirst());
        this.cloner = source -> {
            if (source == null || !this.clonableClass.isAssignableFrom(source.getClass())) {
                throw new DBSystemException("can not clone type '" + (source == null ? null : source.getClass()) + "' expected type is '" + this.clonableClass + "', value is '" + source + "'");
            }
            return cloner.apply(source);
        };
    }

    DataType(Class<?> clazz, List<String> typeSymbol, Class<?> clonableClass) {
        this(clazz, typeSymbol, clonableClass, o -> o);
    }

    DataType(@NotNull List<Class<?>> classes, List<String> typeSymbol, Class<?> clonableClass) {
        this(classes, typeSymbol, clonableClass, o -> o);
    }

    public static @NotNull DataType typeofHValue(@Nullable HValue<?> value) throws IllegalJavaClassStoredException {
        return typeOfRawValue(Objects.requireNonNullElse(value, HValue.EMPTY).data());
    }

    public static @NotNull DataType typeOfRawValue(@Nullable Object instance) throws IllegalJavaClassStoredException {
        return typeofClass(instance == null ? null : instance.getClass());
    }

    public static @NotNull DataType typeofClass(Class<?> clazz) throws IllegalJavaClassStoredException {
        DataType type = javaClassMap.get(clazz);
        if (type == null) {
            throw IllegalJavaClassStoredException.of(clazz == null ? Null.class : clazz);
        }
        return type;
    }

    @Nullable
    public static DataType typeOfSymbol(String symbol) {
        Objects.requireNonNull(symbol);
        return commandSymbolMap.get(symbol.toUpperCase());
    }

    public static Set<DataType> typeOfCloneable(Class<?> cloneableClass) {
        Objects.requireNonNull(cloneableClass);
        return findCloneableDataType(cloneableClass, null);
    }

    static Set<DataType> findCloneableDataType(Class<?> thisClass, Class<?> sonClass) {
        Set<DataType> dataTypes = clonableClassMap.get(thisClass);
        if (dataTypes != null) {
            if (sonClass != null) {
                clonableClassMap.put(sonClass, dataTypes);
            }
            return dataTypes;
        }
        Class<?> superclass = thisClass.getSuperclass();
        if (superclass == Object.class || superclass == null) {
            return null;
        }
        return findCloneableDataType(superclass, thisClass);
    }

    public Set<Class<?>> javaClasses() {
        return javaClasses;
    }

    public ReflectCache<?> reflect() {
        return reflectCache;
    }

    public Collection<String> commandSymbols() {
        return typeSymbol;
    }

    public static boolean support(Class<?> clazz) {
        return javaClassMap.containsKey(clazz);
    }

    @Slf4j
    private static final class Null {
        private Null() {
        }

        public static final Null VALUE = init();

        /**
         * 防止IDEA警告：实例化一个实用类
         */
        private static Null init() {
            try {
                Constructor<Null> constructor = Null.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Throwable e) {
                log.error("unexpected exception: {}", e.toString());
                throw new DBSystemException(e);
            }
        }
    }

    public boolean support(@Nullable Object any) {
        return any != null && javaClasses.contains(any.getClass());
    }

    /**
     * 在内联命令运行后, 可能会返回一个数据库中的值. 如果是数据库中的值
     * 那么如果命令中需要存储这个值, 就会存在一个问题: 一个对象多处引用
     * 可能会导致两个不同的key的value因为引用相同, 导致修改一个其它的都变.
     * 所以需要调用该方法, 克隆对应的值, 只允许在存储时, 存入这个新值, 确保
     * 引用唯一性
     *
     * @param source 源数据
     */
    public Object clone(Object source) {
//        if (source == null || !clonableClass.isAssignableFrom(source.getClass())) {
//            throw new DBSystemException("can not clone type '" + (source == null ? null : source.getClass()) + "' expected type is '" + clonableClass + "', value is '" + source + "'");
//        }
        return cloner.apply(source);
    }

    public boolean unsupportedClone(Object source) {
        return source == null || !clonableClass.isAssignableFrom(source.getClass());
    }
}
