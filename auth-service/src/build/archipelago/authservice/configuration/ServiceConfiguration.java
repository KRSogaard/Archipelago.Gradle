package build.archipelago.authservice.configuration;

import build.archipelago.authservice.services.AuthService;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AuthService authService() {
        return new AuthService() {
            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };
    }
}
