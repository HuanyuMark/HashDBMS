package org.hashdb.ms.support;

import lombok.extern.slf4j.Slf4j;
import org.hashdb.ms.HashDBMSApp;
import org.hashdb.ms.aspect.methodAccess.DisposableCall;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

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
 * Date: 2024/3/7 0:37
 *
 * @author Huanyu Mark
 */
@Slf4j
@Component
public class ConfigSourceFactory implements ObjectFactory<ConfigSource> {
    /**
     * 只有在用户指定了配置文件后, 才会有写的需求, 才需要获取这个对象
     */
    private static ConfigSource configSource;

    @Override
    public @NotNull ConfigSource getObject() throws BeansException {
        return configSource;
    }

    /**
     * @param args 命令行参数
     * @return 转换好的命令行参数
     */
    @DisposableCall
    public static String[] build(String[] args) {
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
        String[] configPrefix = {"--config=", "-native=", "--spring.config.location=", "-Dspring.config.location="};
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
        throw Exit.error(log, STR."illegal config file name '\{fullName}'. suffix must be yml/yaml/properties", "illegal suffix");
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
