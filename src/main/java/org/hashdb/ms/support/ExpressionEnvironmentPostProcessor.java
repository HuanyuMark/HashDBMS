package org.hashdb.ms.support;

import org.hashdb.ms.util.UnmodifiableCollections;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.*;
import java.util.stream.Stream;

/**
 * Date: 2024/2/23 0:23
 *
 * @author Huanyu Mark
 */
public abstract class ExpressionEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().stream()
                .filter(ps -> ps instanceof OriginTrackedMapPropertySource)
                .flatMap(ps -> {
                    var ops = (OriginTrackedMapPropertySource) ps;
                    var source = ops.getSource();
                    var properties = evaluateMap(source);
                    if (source == properties) {
                        return Stream.empty();
                    }
                    var mutable = replaceWithMutable();
                    return Stream.of(new OriginTrackedMapPropertySource(ops.getName(),
                            mutable ? properties : Collections.unmodifiableMap(properties), !mutable));
                }).forEach(newPs -> {
                    environment.getPropertySources().replace(newPs.getName(), newPs);
                });
    }

    private Map<String, Object> evaluateMap(Map<String, Object> source) {
        Map<String, Object> result = source;
        for (var entry : source.entrySet()) {
            var originTrackedValue = (OriginTrackedValue) entry.getValue();
            var value = originTrackedValue.getValue();
            if (value instanceof String express) {
                // 使用Jep运行一遍这些值, 看看是不是数学表达式, 如果是, 则替换其值
                var temp = evaluate(express);
                if (express.equals(temp)) {
                    continue;
                }
                if (UnmodifiableCollections.isUnmodifiableMap(result.getClass())) {
                    result = new LinkedHashMap<>(result);
                }
                result.put(entry.getKey(), OriginTrackedValue.of(temp, originTrackedValue.getOrigin()));
                continue;
            }
            // propertySource里的配置项都经过了扁平化处理, 值不会是集合类型
//            if (value instanceof Collection<?> native) {
//                var temp = evaluateCollection(((Collection<Object>) native));
//                if (temp == source) {
//                    continue;
//                }
//                if (UnmodifiableCollections.isUnmodifiableMap(result.getClass())) {
//                    result = new LinkedHashMap<>(result);
//                }
//                result.put(entry.getKey(), OriginTrackedValue.of(temp, originTrackedValue.getOrigin()));
//                continue;
//            }
//            if (value.getClass().isArray()) {
//                evaluateArray(value);
//            }
        }
        return result;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private <C extends Collection<Object>> C evaluateCollection(C source) {
        if (source instanceof Map<?, ?> m) {
            return (C) evaluateMap((Map<String, Object>) m);
        }
        if (source instanceof Set<?> || source instanceof List<?>) {
            return evaluateListOrSet(source);
        }
        throw new RuntimeException("Unsupported collection type: " + source.getClass());
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private <C extends Collection<Object>> C evaluateListOrSet(C source) {
        boolean isList = source instanceof List<?>;
        C result = source;
        var itr = source.iterator();
        var marks = new ArrayList<Mark>();
        for (int i = 0; itr.hasNext(); i++) {
            var value = itr.next();
            if (value instanceof String expression) {
                var temp = evaluate(expression);
                if (temp.equals(expression)) {
                    continue;
                }
                var resultClass = result.getClass();
                result = (C) ExpressionEnvironmentPostProcessor.checkAndOptionalReplace(result, resultClass);
                if (isList) {
                    marks.add(new Mark(i, temp));
                } else {
                    result.add(temp);
                }
            }
            if (value instanceof Collection<?> collection) {
                var temp = evaluateCollection((Collection<Object>) collection);
                if (temp == source) {
                    continue;
                }
                result = (C) ExpressionEnvironmentPostProcessor.checkAndOptionalReplace(result, result.getClass());
                if (isList) {
                    marks.add(new Mark(i, temp));
                } else {
                    result.add(temp);
                }
            }
            if (value.getClass().isArray()) {
                evaluateArray(value);
            }
        }
        if (isList) {
            if (result instanceof RandomAccess) {
                for (var mark : marks) {
                    ((List<Object>) result).set(mark.iterationCount(), mark.value());
                }
            }
            var listLtr = ((List<Object>) result).listIterator();
            int i = -1;
            for (var mark : marks) {
                ++i;
                if (mark.iterationCount() != i) {
                    listLtr.next();
                    continue;
                }
                listLtr.set(listLtr.next());
            }
        }
        return result;
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    private static <C extends Collection<Object>> C checkAndOptionalReplace(C result, Class<? extends C> resultClass) {
        if (!UnmodifiableCollections.isUnmodifiableCollection(resultClass)) {
            return result;
        }
        if (UnmodifiableCollections.isUnmodifiableSet(resultClass)) {
            result = (C) new HashSet<>(result);
        } else if (UnmodifiableCollections.isUnmodifiableList(resultClass)) {
            result = (C) new ArrayList<>(result);
        } else {
            throw new RuntimeException("Unsupported collection type: " + resultClass);
        }
        return result;
    }

    @Deprecated
    private void evaluateArray(Object source) {
    }

    @NotNull
    protected abstract Object evaluate(String expression);

    /**
     * @return 如果为true, 则表达式运行后{@link #evaluate(String)}.
     * 原有的PropertySource被替换后, 底层依赖的Map是不是可变的.
     * 这一点会影响到多个{@link ExpressionEnvironmentPostProcessor} 间的运行.
     * 如果使用了多个{@link ExpressionEnvironmentPostProcessor},
     * 最好就重写该方法, 减少性能消耗
     */
    protected boolean replaceWithMutable() {
        return false;
    }

    @Deprecated
    private record Mark(int iterationCount, Object value) {
    }
}
