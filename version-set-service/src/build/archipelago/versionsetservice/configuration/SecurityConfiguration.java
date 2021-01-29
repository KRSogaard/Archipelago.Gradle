package build.archipelago.versionsetservice.configuration;

import build.archipelago.versionsetservice.controllers.HealthController;
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
                .mvcMatchers(HttpMethod.GET, "/account/**").hasAuthority("SCOPE_http://versionsetservice.archipelago.build/read")
                .mvcMatchers(HttpMethod.POST, "/account/**").hasAuthority("SCOPE_http://versionsetservice.archipelago.build/write")
                .and().cors()
                .and().oauth2ResourceServer().jwt();
    }
}