package org.hashdb.ms;

import org.hashdb.ms.communication.ConnectionServer;
import org.hashdb.ms.data.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.FileNotFoundException;

/**
 * Date: ${DATE} ${TIME}
 *
 * @author huanyuMake-pecdle
 * @version 0.0.1
 */
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // 因为要自定义设置所以配了, 暴露代理是为了, 可以在本类拿到代理对象,从而在本类或跨类调用代理对象
@EnableConfigurationProperties
public class HashDBMSApp {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) throws FileNotFoundException {
        context = SpringApplication.run(HashDBMSApp.class, args);
//        ConnectionServer server = context.getBean(ConnectionServer.class);
//        server.start(3050).join();
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}