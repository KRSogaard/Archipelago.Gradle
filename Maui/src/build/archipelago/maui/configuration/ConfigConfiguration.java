package build.archipelago.maui.configuration;

import build.archipelago.maui.utils.*;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.*;

@Configuration
public class ConfigConfiguration {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() throws IOException {
        // TODO: Make this better, have a setup command
        if (!Files.exists(SystemUtil.getMauiPath())) {
            Files.createDirectory(SystemUtil.getMauiPath());
        }
        Path configPath = SystemUtil.getMauiPath().resolve("maui.config");
        if (!Files.exists(configPath)) {
            ConfigUtil.writeDefaultConfig(configPath);
        }

        PropertySourcesPlaceholderConfigurer properties =
                new PropertySourcesPlaceholderConfigurer();
        properties.setLocation(new FileSystemResource(configPath));
        properties.setIgnoreResourceNotFound(false);
        return properties;
    }
}
