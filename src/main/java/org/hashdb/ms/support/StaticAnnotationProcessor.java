package org.hashdb.ms.support;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Date: 2024/2/28 15:33
 * <p>
 * 默认情况下, 扫描{@link SpringBootApplication} 启动类包路径下的所有符合条件的类, 实现静态注入.
 * 副作用就是因为扫描了所有类, 导致常量池中的class对象在一开始就加载完
 * 有些程序运行时不会动态加载的类会被加载到.
 * 但也有好处, 就是提前预热了jvm的类加载, 程序不会因为类加载而被暂停
 * <p>
 * Note: 请注意, 如果被该处理器加载的类被其它类加载器重复加载,
 * 那么静态变量及方法可能不会被注入. 需要手动调用 {@link #doStaticAutowire}
 * 来重新加载(但是因为只有同一个类加载器加载的类的实例能互相访问, 所以这个问题出现的几率非常非常小)
 *
 * @author Huanyu Mark
 * @see StaticAutowired
 * @see StaticScanIgnore
 */
@Slf4j
@Order(1)
@Component
public class StaticAnnotationProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
        doStaticAutowire(event.getApplicationContext());
    }

    public void doStaticAutowire(ApplicationContext context) {
        var appBeans = context.getBeansWithAnnotation(SpringBootApplication.class);
        if (appBeans.size() != 1) {
            throw new BeansException(STR."require unique bean annotated with \{SpringBootApplication.class}, but found \{appBeans}") {
            };
        }
        var appBean = appBeans.values().toArray()[0];
        doStaticAutowire(context, appBean.getClass().getPackage().getName());
    }

    public void doStaticAutowire(ApplicationContext context, String packageName) {
        info(() -> "inject   start. scan all class and inject static method/field");
        var provider = new ClassPathScanningCandidateClassProvider();
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        provider.addExcludeFilter(new AnnotationTypeFilter(StaticScanIgnore.class));
        var beanDefinitions = provider.findCandidateComponents(packageName);
        for (var beanDefinition : beanDefinitions) {
            Class<?> beanClazz;
            try {
                beanClazz = Objects.requireNonNullElseGet(context.getClassLoader(), () -> Thread.currentThread().getContextClassLoader())
                        .loadClass(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException ignore) {
                continue;
            }
            // inject field
            Arrays.stream(beanClazz.getDeclaredFields())
                    .filter(f -> Modifier.isStatic(f.getModifiers()))
                    .filter(f -> f.isAnnotationPresent(StaticAutowired.class))
                    .filter(f -> {
                        if (Modifier.isFinal(f.getModifiers())) {
                            throw Exit.error(log, STR."can not autowire static final field '\{toString(f)}'", "field is final");
                        }
                        return true;
                    })
                    .forEach(injectableField -> {
                        ReflectionUtils.makeAccessible(injectableField);
                        Object value;
                        try {
                            try {
                                value = context.getBean(injectableField.getType());
                            } catch (NoUniqueBeanDefinitionException e) {
                                value = context.getBean(injectableField.getName(), injectableField.getType());
                            }
                        } catch (NoSuchBeanDefinitionException e) {
                            StaticAutowired staticAutowired;
                            if (e instanceof NoUniqueBeanDefinitionException ex ||
                                    (staticAutowired = AnnotationUtils.findAnnotation(injectableField, StaticAutowired.class)) == null ||
                                    staticAutowired.required()) {
                                error(STR."inject failure. '\{toString(injectableField)}'. fail to find bean. cause: {}", e.getMessage());
                                throw Exit.exception();
                            }
                            return;
                        } catch (BeansException e) {
                            error(STR."inject failure. '\{toString(injectableField)}'. fail to find bean. cause: {}", e.getMessage());
                            throw Exit.exception();
                        }

                        try {
                            injectableField.set(null, value);
                            info(() -> STR."inject success. '\{toString(injectableField)}'");
                        } catch (Exception e) {
                            error(STR."inject failure. '\{toString(injectableField)}'. set field throw", e);
                            throw Exit.exception();
                        }
                    });
            // inject method
            Arrays.stream(ReflectionUtils.getDeclaredMethods(beanClazz))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> m.isAnnotationPresent(StaticAutowired.class))
                    .forEach(injectableMethod -> {
                        ReflectionUtils.makeAccessible(injectableMethod);
                        var parameters = injectableMethod.getParameters();
                        try {
                            if (parameters.length == 0) {
                                ReflectionUtils.invokeMethod(injectableMethod, null);
                                info(() -> STR."run    success. '\{toNoArgsString(injectableMethod)}'");
                                return;
                            }
                            var injectResources = Arrays.stream(parameters).map(p -> {
                                try {
                                    try {
                                        return context.getBean(p.getType());
                                    } catch (NoUniqueBeanDefinitionException e) {
                                        return context.getBean(p.getName(), p.getType());
                                    }
                                } catch (NoSuchBeanDefinitionException e) {
                                    StaticAutowired staticAutowired;
                                    if (e instanceof NoUniqueBeanDefinitionException ex ||
                                            (staticAutowired = AnnotationUtils.findAnnotation(p, StaticAutowired.class)) == null ||
                                            staticAutowired.required()
                                    ) {
                                        error(STR."inject failure. '\{toString(injectableMethod, parameters)}'. find bean failure: {}", e.getMessage());
                                        throw Exit.exception();
                                    }
                                    return null;
                                } catch (BeansException e) {
                                    error(STR."inject failure. '\{toString(injectableMethod, parameters)}'. find bean failure: {}", e.getMessage());
                                    throw Exit.exception();
                                }
                            }).toArray();
                            injectableMethod.invoke(null, injectResources);
                            info(() -> STR."inject success. '\{toString(injectableMethod, parameters)}'");
                        } catch (BeansException e) {
                            error(STR."inject failure. '\{toString(injectableMethod, parameters)}'. find bean failure: {}", e.getMessage());
                            throw Exit.exception();
                        } catch (Exception e) {
                            error(STR."inject failure. '\{toString(injectableMethod, parameters)}'. method throw", e);
                            throw Exit.exception();
                        }
                    });
        }
        info(() -> "inject    done.");
    }

    private static String toString(Field field) {
        return STR."\{toString(field.getType())} \{toString(field.getDeclaringClass())}.\{field.getName()}";
    }

    private static String toString(Method method) {
        return toString(method, method.getParameters());
    }

    private static String toString(Method method, Parameter[] parameters) {
        return STR."\{toString(method.getDeclaringClass())}.\{method.getName()}\{toString(parameters)}";
    }

    private static String toNoArgsString(Method method) {
        return STR."\{toString(method.getDeclaringClass())}.\{method.getName()}()";
    }

    private static String toString(Parameter[] parameters) {
        if (parameters.length == 0) {
            return "()";
        }
        var builder = new StringBuilder();
        var firstParameter = parameters[0];
        builder.append('(').append(toString(firstParameter.getType()));
        for (int i = 1; i < parameters.length; i++) {
            var parameter = parameters[i];
            builder.append(", ");
            builder.append(toString(parameter.getType()));
        }
        return builder.append(')').toString();
    }

    private static String toString(Class<?> clazz) {
        String canonicalName = clazz.getName();
        return canonicalName.substring(canonicalName.lastIndexOf(".") + 1);
    }

    private static void info(Supplier<String> msg) {
//        System.out.println(msg);
    }

    private static void info(String msg) {
        System.out.println(msg);
    }

    private static void error(String msg, Object... args) {
        log.info(msg, args);
    }

    private static class ClassPathScanningCandidateClassProvider extends ClassPathScanningCandidateComponentProvider {
        public ClassPathScanningCandidateClassProvider() {
            super(false);
        }

        /**
         * 允许扫描抽象类
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return !beanDefinition.getMetadata().isInterface();
        }
    }
}
