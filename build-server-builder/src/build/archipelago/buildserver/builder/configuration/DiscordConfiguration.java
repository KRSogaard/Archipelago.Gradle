package build.archipelago.buildserver.builder.configuration;

import build.archipelago.buildserver.builder.notifications.DiscordNotificationProvider;
import build.archipelago.buildserver.builder.notifications.NotificationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class DiscordConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public NotificationProvider getNotificationProvider(@Value("${discord.webhook}") String webhook) {
        return new DiscordNotificationProvider(webhook);
    }
}
