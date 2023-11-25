package org.hashdb.ms;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.util.JacksonSerializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.*;

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

    @Slf4j
    static final class Person implements Comparable<Person> {
        private int age;

        Person(int age) {
            this.age = age;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public int compareTo(@NotNull Person o) {
            return age - o.age;
        }

        @Override
        public String toString() {
            return "Person[" +
                    "age=" + age + ']';
        }
    }

    public static void main(String[] args) throws InterruptedException, ClassNotFoundException, JsonProcessingException {
        context = SpringApplication.run(HashDBMSApp.class, args);
        Database database = new Database(1, "main", new Date(), Map.of("list", new LinkedList<>()));
        database.startDatabaseDaemon().join();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String command = scanner.nextLine();
                SupplierCompileStream compileStream = SupplierCompileStream.open(database, command);
                SupplierCtx supplierCtx = compileStream.compile();
                database.submitOpsTaskSync(supplierCtx.compileResult());
                Object result = supplierCtx.compileResult().result();
                System.out.println("result:");
                System.out.println(JacksonSerializer.stringfy(result == null ? "null": result));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}