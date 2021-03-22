package build.archipelago.packageservice.configuration;

import build.archipelago.packageservice.models.PackageDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.*;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class CacheConfiguration {

    @Bean(name = "publicPackageAccountCache")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Cache<String, String> publicPackageAccountCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .build();
    }

    @Bean(name = "packageListCache")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Cache<String, List<PackageDetails>> packageListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .build();
    }
}
