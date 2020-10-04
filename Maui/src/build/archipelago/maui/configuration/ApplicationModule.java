package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.MauiCommand;
import build.archipelago.maui.utils.*;
import com.google.inject.*;
import com.google.inject.name.Names;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ApplicationModule extends AbstractModule {

    @Override
    protected void configure() {
        try {
            // TODO: Make this better, have a setup command
            if (!Files.exists(SystemUtil.getMauiPath())) {
                    Files.createDirectory(SystemUtil.getMauiPath());
            }
            Path configPath = SystemUtil.getMauiPath().resolve("maui.config");
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
        install(new CommandConfiguration());
    }

    @Provides
    public MauiCommand mauiCommand() {
        return new MauiCommand();
    }
}
