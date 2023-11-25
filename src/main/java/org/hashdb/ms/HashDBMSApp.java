package org.hashdb.ms;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.data.DataType;
import org.hashdb.ms.util.JacksonSerializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.configurationprocessor.json.JSONStringer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.awt.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

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
//        String target = "SET p1 { \"name\":\"张三  zhangSan\",  \"age\": 18 } p2 {\"name\":\"王五\", \"age\": 24}";
//        JSONStringer stringer = new JSONStringer();
//        String s = "(--123abc)";
//        System.out.println(s.substring(0,s.length()-1));
//        SpelParserConfiguration configuration = new SpelParserConfiguration();
//        SpelExpressionParser parser = new SpelExpressionParser();
//        SpelExpression expression = parser.parseRaw("{'name':'张三','ages':13}");
//        log.info("result: {}",expression.compileExpression());
//        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
//        Person p1 = new Person(10);
//        List<Integer> list = List.of(1, 2, 3, 4, 5);
//        evaluationContext.setVariable("age", 5);
//        Person person = new Person(5);
//        log.info("result: {}. root: {}",expression.getValue(evaluationContext),evaluationContext.lookupVariable("age"));
        //        evaluationContext.setVariable("p3", new Person(2));
//        context = SpringApplication.run(HashDBMSApp.class, args);
//        ConnectionServer server = context.getBean(ConnectionServer.class);
//        server.start(3050).join();
        Object parse = JacksonSerializer.parse("null");
        log.info("parse: {}", parse);
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}