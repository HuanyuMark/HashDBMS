package org.hashdb.ms;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.net.StartServerEvent;
import org.hashdb.ms.util.JsonService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Date: ${DATE} ${TIME}
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 因为要自定义设置所以配了, 暴露代理是为了, 可以在本类拿到代理对象,从而在本类或跨类调用代理对象
@EnableConfigurationProperties
public class HashDBMSApp {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) throws JsonProcessingException {
        context = SpringApplication.run(HashDBMSApp.class, args);
        JsonService.loadConfig();
        // 发布一个事件, 让其他模块可以感知到spring容器已经准备好了, context 已经有值了
        // 在idea中,点击左边的耳机符号,可以跳转到监听这个事件的事件监听器
//        String json = "{\"id\":\"50281e23-a4a3-4667-a5a6-4071394ce26d\",\"timestamp\":1701518554097,\"data\":null,\"type\":\"AUTH\"}";
        context.publishEvent(new StartServerEvent());
//        JsonService.parse(json,Message.class);
//        JsonService.COMMON.registerSubtypes(APerson.class);
//        Object o = JsonService.parse("{\"name\":\"王五\"}", Person.class);
//        System.out.println(o);
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}