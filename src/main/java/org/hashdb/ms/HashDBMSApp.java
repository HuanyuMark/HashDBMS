package org.hashdb.ms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.util.JsonService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.IOException;

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

    @Data
    public static class MYConfig {
        String file;

        public MYConfig() {
        }

        public MYConfig(String file) {
            this.file = file;
        }
    }

    public static class MyYamlConfig {
        @JsonProperty
        private String file = System.getProperty("user.dir");
    }

    public static class MyYamlSubConfig extends MyYamlConfig {
        @JsonProperty
        private String aabb = "aabb";

        @Override
        public String toString() {
            return "MyYamlSubConfig{" +
                    "aabb='" + aabb + '\'' +
                    '}';
        }
    }

    public static void main(String[] args) throws IOException {
        context = SpringApplication.run(HashDBMSApp.class, args);
        // 发布一个事件, 让其他模块可以感知到spring容器已经准备好了, context 已经有值了
        // 在idea中,点击左边的耳机符号,可以跳转到监听这个事件的事件监听器
//        context.publishEvent(new StartServerEvent());
        String userDir = System.getProperty("user.dir");
        log.info("dir: " + userDir);
//
//        String file = userDir + "/my.yml";
//        YAMLMapper yamlMapper = new YAMLMapper();
//
        MyYamlSubConfig myYamlConfig = new MyYamlSubConfig();
        String stringfy = JsonService.stringfy(myYamlConfig);
        MyYamlSubConfig parse = JsonService.parse(stringfy, MyYamlSubConfig.class);
//        myYamlConfig.setMyConfigs(List.of(new MYConfig("zxczxc"), new MYConfig("45123")));
//        myYamlConfig.setStrings(List.of("123", "456"));
//        String toSave = yamlMapper.writeValueAsString(myYamlConfig);
//        log.info("toSave: {}", toSave);
//        try (FileWriter fileWriter = new FileWriter(file)) {
//            fileWriter.append(toSave);
//        }
//        MyYamlConfig c = yamlMapper.readValue(new FileReader(file), MyYamlConfig.class);
//        log.info("MyYamlConfig: {}", c);
        log.info("result: {} parse: {}", stringfy, parse);
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }
}