package build.archipelago.maui.configuration;

import build.archipelago.maui.commands.MauiCommand;
import org.springframework.context.annotation.*;

@Configuration
public class BaseCommandConfiguration {

    @Bean
    public MauiCommand mauiCommand() {
        return new MauiCommand();
    }
}
