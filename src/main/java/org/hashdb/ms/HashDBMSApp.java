package org.hashdb.ms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.config.DBServerConfig;
import org.hashdb.ms.event.ApplicationContextLoadedEvent;
import org.hashdb.ms.manager.DBSystem;
import org.hashdb.ms.support.ConfigSource;
import org.hashdb.ms.support.Exit;
import org.hashdb.ms.support.YamlComment;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Date: ${DATE} ${TIME}
 *
 * @author huanyuMake-pecdle
 */
@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy(exposeProxy = true) // 因为要自定义设置所以配了, 暴露代理是为了, 可以在本类拿到代理对象,从而在本类或跨类调用代理对象
public class HashDBMSApp {

    @Getter
    private static DBServerConfig dbServerConfig;
    @Getter
    private static DBSystem dbSystem;
    private static boolean asynchronousClose = false;
    private static ConfigurableApplicationContext context;

    /**
     * 只有在用户指定了配置文件后, 才会有写的需求, 才需要获取这个对象
     */
    @Getter
    private static ConfigSource configSource;

    @Data
    @Setter
    public static class MyObj {
        @JsonProperty
        private String okk = "ok";

        private String nook = "nook";

        @YamlComment("getNook comment")
        public String getNook() {
            return nook;
        }

        @YamlComment("getOkk comment")
        public String getOkk() {
            return okk;
        }
    }

    /**
     * @param args 命令行参数
     *             现在支持: --config=[URL or file path], URL支持file://, http://, https://, ftp://协议
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // 发布一个事件, 让其他模块可以感知到spring容器已经准备好了, context 已经有值了
//        context.publishEvent(new ApplicationContextLoadedEvent(SpringApplication.run(HashDBMSApp.class, prepareArgs(args))));
        File file = new File("source.yaml");
        ConfigSource source = ConfigSource.open(file);
        MyObj obj = new MyObj();
        source.update(ConfigSource.Mark.REPLICATION, obj);
        Thread.sleep(2000);
    }

    @Order(1)
    @EventListener(ApplicationContextLoadedEvent.class)
    public void loadStatic(ApplicationContextLoadedEvent event) {
        context = event.ctx();
        if (asynchronousClose) {
            context.close();
            return;
        }
        dbServerConfig = context.getBean(DBServerConfig.class);
    }

    public static ConfigurableApplicationContext ctx() {
        return context;
    }

    private static String[] prepareArgs(String[] args) {
        boolean useEnv = false;
        if (System.getenv("SPRING_CONFIG_NAME") != null) {
            log.error("System environment variables 'SPRING_CONFIG_NAME' is specified, please use VM option: '--spring.config.name'");
            useEnv = true;
        }
        if (System.getenv("SPRING_CONFIG_LOCATION") != null) {
            log.error("System environment variables 'SPRING_CONFIG_LOCATION' is specified, please use VM option: '--spring.config.location'");
            useEnv = true;
        }
        if (useEnv) {
            throw Exit.exception();
        }
        String[] configPrefix = {"--config=", "-c=", "--spring.config.location=", "-Dspring.config.location="};
        String[] filterPrefix = {"--spring.config.name=", "-Dspring.config.name="};
        String configFileDir = null;
        var res = new ArrayList<String>(args.length);
        for (String arg : args) {
            boolean matched = false;
            for (String prefix : filterPrefix) {
                if (arg.startsWith(prefix)) {
                    log.warn(STR."invalid option '\{arg}'");
                    matched = true;
                    break;
                }
            }
            for (String prefix : configPrefix) {
                if (arg.startsWith(prefix)) {
                    configFileDir = arg.substring(prefix.length());
                    matched = true;
                }
            }
            if (!matched) {
                res.add(arg);
            }
        }
        String fullname;
        String name;
        if (configFileDir == null) {
            log.warn("no configuration file is specified. use default configuration");
            name = "--spring.config.name=application";
            configSource = ConfigSource.open(HashDBMSApp.class.getClassLoader().getResource("application.yml"));
        } else {
            File configFilePath;
            try {
                var url = URI.create(configFileDir).toURL();
                configSource = ConfigSource.open(url);
                configFilePath = tempFile();
                try {
                    configSource.transferTo(configFilePath);
                } catch (IOException e) {
                    log.error("can not create temp file for URL '{}'. cause: {}", url, e.getMessage());
                    throw Exit.exception();
                }
            } catch (MalformedURLException | IllegalArgumentException e) {
                log.trace("'{}' is not a url like string", configFileDir);
                // 解析绝对路径,相对路径为文件
                try {
                    configFilePath = Path.of(configFileDir).normalize().toAbsolutePath().toFile();
                } catch (InvalidPathException ex) {
                    log.error("illegal config file path '{}'. cause: {}", configFileDir, ex.getMessage());
                    throw Exit.exception();
                }
                if (configFilePath.isDirectory()) {
                    log.error("config file should be a file or URL like");
                    throw Exit.exception();
                }
                configSource = ConfigSource.open(configFilePath);
            }
            var location = STR."--spring.config.location=\{configFilePath.getParent()}\\";
            name = STR."--spring.config.name=\{extractProfileName(configFilePath.getName())}";
            res.add(location);
        }
        res.add(name);
        return res.toArray(String[]::new);
    }

    private static String extractProfileName(String fullName) {
        // 正则表达式，用于匹配整个文件名，包括扩展名
        var pattern = Pattern.compile("^.*\\.((yml)|(yaml)|(properties))$");
        var matcher = pattern.matcher(fullName);
        if (matcher.matches()) {
            // 获取完整的文件名（已知是匹配的）
            var fullFileName = matcher.group();
            // 找到最后一个点的位置
            int dotIndex = fullFileName.lastIndexOf(".");
            if (dotIndex != -1) {
                // 截取掉扩展名部分，得到不带扩展名的文件名
                return fullFileName.substring(0, dotIndex);
            }
        }
        log.error("illegal config file name '{}'. suffix must be yml/yaml/properties", fullName);
        throw Exit.exception();
    }

    public static void exit(int status) {
        if (context == null) {
            log.warn("app context is not loaded");
            asynchronousClose = true;
        } else {
            context.close();
        }
        System.exit(status);
    }

    private static File tempFile() {
        try {
            return File.createTempFile(STR."hashdb_remote_config_pid_\{ManagementFactory.getRuntimeMXBean().getPid()}", "yml");
        } catch (IOException e) {
            log.error("can not create remote temp file. cause: {}", e.getMessage());
            throw Exit.exception();
        }
    }
}