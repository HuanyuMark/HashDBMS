package org.hashdb.ms;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.support.ConfigSourceFactory;
import org.hashdb.ms.support.Exit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Date: ${DATE} ${TIME}
 *
 * @author Huanyu Mark
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy(exposeProxy = true) // 因为要自定义设置所以配了, 暴露代理是为了, 可以在本类拿到代理对象,从而在本类或跨类调用代理对象
public class HashDBMSApp implements ApplicationListener<ContextRefreshedEvent> {
    private static ApplicationContext context;

    private static boolean asynchronousClose = false;

    public record Other(String text) {

    }

    public static class MyClass extends ArrayList<String> {

    }


    public record O(String name, long[] array, Object any) {
    }

    /**
     * @param args 命令行参数
     *             现在支持: --config=[URL or file path], URL支持file://, http://, https://, ftp://协议
     */
    public static void main(String[] args) throws IOException {
        ResolvableType type = ResolvableType.forClass(MyClass.class);
        ResolvableType generic = type.getSuperType().getGeneric(0);
        Class<?> bodyClass = generic.resolve();
        System.out.println(bodyClass);
        //        BsonFactory factory = new BsonFactory();
////        factory.enable(BsonGenerator.Feature.ENABLE_STREAMING);
//        ObjectMapper mapper = new ObjectMapper(factory);
//        long[] longs = {1, 2, 3, 4, 5, 6, 7, 8, 9};
//        O pojo = new O("myName", longs, new Other("this is text"));
//        byte[] bytes = mapper.writeValueAsBytes(pojo);
//        System.out.println(Arrays.toString(bytes));
//        BsonParser parser = factory.createJsonParser(bytes);

//        while (parser.hasCurrentToken()) {
//            System.out.println(parser.nextToken());
//            System.out.println(parser.getCurrentName());
//            System.out.println(parser.getValueAsString());
//            System.out.println(Arrays.toString(parser.getBinaryValue()));
//        }
        SpringApplication.run(HashDBMSApp.class, ConfigSourceFactory.build(args));
//        ResolvableType resolvableType = ResolvableType.forInstance(new Ref<String>() {
//        });
    }


    public native void test();

    interface Ref<E> {
    }


    @Order(10)
    @EventListener(ApplicationContext.class)
    public void doAsynchronousClose() {
        if (asynchronousClose) {
            if (context instanceof ConfigurableApplicationContext c) {
                c.close();
            }
            throw Exit.normal();
        }
    }

    public static void exit(int status) {
        if (context == null) {
            log.warn("app context is not loaded");
            asynchronousClose = true;
        } else if (context instanceof ConfigurableApplicationContext c) {
            c.close();
        }
        System.exit(status);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        context = event.getApplicationContext();
        // 发布一个事件, 让其他模块可以感知到spring容器已经准备好了, context 已经有值了
        context.publishEvent(context);
    }
}