package build.archipelago.packageservice.configuration;

import build.archipelago.packageservice.controllers.HealthController;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .mvcMatchers(HealthController.HEALTH_PATH).permitAll()
                .mvcMatchers(HttpMethod.GET, "/account/**").hasAuthority("SCOPE_http://packageservice.archipelago.build/read")
                .mvcMatchers(HttpMethod.POST, "/account/**").hasAuthority("SCOPE_http://packageservice.archipelago.build/write")
                .and().cors()
                .and().oauth2ResourceServer().jwt();
    }
}
