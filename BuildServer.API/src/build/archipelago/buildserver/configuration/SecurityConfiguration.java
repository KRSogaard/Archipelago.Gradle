package build.archipelago.buildserver.configuration;

import build.archipelago.buildserver.controllers.HealthController;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.*;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers(HealthController.HEALTH_PATH).permitAll()
                .mvcMatchers(HttpMethod.POST, "/build").hasAuthority("SCOPE_http://buildserver-api.archipelago.build/create")
                .and().cors()
                .and().oauth2ResourceServer().jwt();
    }
}
