package org.hashdb.ms.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;

/**
 * Date: 2024/2/22 23:58
 *
 * @author Huanyu Mark
 * @version 0.0.1
 */
@Slf4j
public class IntegrationConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var configName = "META-INF/hash-app-base.properties";
        var configResource = (application.getResourceLoader() == null ? new DefaultResourceLoader() : application.getResourceLoader()).getResource(configName);
        var loader = new PropertiesPropertySourceLoader();
        try {
            for (var propertySource : loader.load(configName, configResource)) {
                environment.getPropertySources().addLast(propertySource);
            }
        } catch (IOException e) {
            log.error("can not load integration config. cause: " + e.getMessage());
            throw Exit.exception();
        }
    }
}
