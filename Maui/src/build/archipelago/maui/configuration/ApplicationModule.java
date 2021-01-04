package build.archipelago.maui.configuration;

import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.utils.ConfigUtil;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        try {
            // This is ok as we don't use the current dir
            SystemPathProvider systemPathProvider = new SystemPathProvider();
            // TODO: Make this better, have a setup command
            if (!Files.exists(systemPathProvider.getMauiPath())) {
                    Files.createDirectory(systemPathProvider.getMauiPath());
            }
            Path configPath = systemPathProvider.getMauiPath().resolve("maui.config");
            if (!Files.exists(configPath)) {
                ConfigUtil.writeDefaultConfig(configPath);
            }

            Properties properties = new Properties();
            properties.load(new FileInputStream(configPath.toFile()));
            Names.bindProperties(binder(), properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        install(new ServiceConfiguration());
        install(new ActionConfiguration());
        install(new CommandConfiguration());
    }


}
