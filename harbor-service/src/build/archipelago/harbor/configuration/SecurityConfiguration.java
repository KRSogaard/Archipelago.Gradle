package build.archipelago.harbor.configuration;

import build.archipelago.harbor.controllers.HealthController;
import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.*;

@Configuration
//@EnableWebSecurity
public class SecurityConfiguration { //extends WebSecurityConfigurerAdapter {

//    @Override
//    public void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .mvcMatchers("/auth/login").permitAll()
//                //.mvcMatchers("/package/**").hasAuthority("SCOPE_http://harbor.archipelago.build/packages")
//                .anyRequest().authenticated()
//                .and().cors()
//                .and().oauth2ResourceServer().jwt();
//    }
}