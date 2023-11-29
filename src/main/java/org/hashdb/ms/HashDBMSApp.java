package org.hashdb.ms;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.compiler.SupplierCompileStream;
import org.hashdb.ms.compiler.keyword.ctx.supplier.SupplierCtx;
import org.hashdb.ms.data.Database;
import org.hashdb.ms.data.HValue;
import org.hashdb.ms.data.PlainPair;
import org.hashdb.ms.data.StorableHValue;
import org.hashdb.ms.exception.DatabaseClashException;
import org.hashdb.ms.exception.NotFoundDatabaseException;
import org.hashdb.ms.sys.DBSystem;
import org.hashdb.ms.util.JacksonSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.*;
import java.util.stream.Collectors;

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

    public static void main(String[] args) throws InterruptedException, ClassNotFoundException, JsonProcessingException {
        context = SpringApplication.run(HashDBMSApp.class, args);
        DBSystem dbSystem = context.getBean(DBSystem.class);
        Database database = null;
        try {
            database = dbSystem.getDatabase(1);
        } catch (NotFoundDatabaseException ignore) {}
        if(database == null) {
            database = new Database(
                    1,
                    "main",
                    new Date(),
                    Map.of("ls", new StorableHValue<>(new LinkedList<>(),null,null))
            );
            database.startDaemon().join();
            dbSystem.addDatabase(database);
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String command = scanner.nextLine();
                if(command.equals("exit")){
                    System.exit(0);
                }
                SupplierCompileStream compileStream = SupplierCompileStream.open(database, command);
                SupplierCtx supplierCtx = compileStream.compile();
                database.submitOpsTask(supplierCtx.compileResult());
                Object result = supplierCtx.compileResult().result();
                Object normalizeValue = normalizeValue(result);
                System.out.println(JacksonSerializer.stringfy(normalizeValue == null ? "null" : normalizeValue));
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public static Object normalizeValue(Object result){
        if(result instanceof HValue<?> hValue){
            return hValue.data();
        }
        if(result instanceof List<?> ls){
            return ls.stream().map(HashDBMSApp::normalizeValue).toList();
        }
        if(result instanceof Set<?> ls){
            return ls.stream().map(HashDBMSApp::normalizeValue).toList();
        }
        if(result instanceof Map<?,?> map){
            return map.entrySet().stream().map((entry)-> new PlainPair<>(entry.getKey(),normalizeValue(entry.getValue()))).collect(Collectors.toMap(PlainPair::key,PlainPair::value));
        }
        return result;
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}