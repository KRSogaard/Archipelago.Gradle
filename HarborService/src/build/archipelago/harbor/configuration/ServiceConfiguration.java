package build.archipelago.harbor.configuration;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageServiceClient packageServiceClient(
            @Value("${endpoints.package-service}") String packageServiceEndpoint,
            @Value("${oauth.client-id}") String clientId,
            @Value("${oauth.client-secret}") String clientSecret
    ) {
        return new RestPackageServiceClient(packageServiceEndpoint, clientId, clientSecret);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VersionSetServiceClient versionSetService(
            @Value("${endpoints.versionset-service}") String vsServiceEndpoint,
            @Value("${oauth.client-id}") String clientId,
            @Value("${oauth.client-secret}") String clientSecret) {
        return new RestVersionSetServiceClient(vsServiceEndpoint, clientId, clientSecret);
    }

    @Bean("tempDir")
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Path getTempDir() throws IOException {
        Path buildDir = Paths.get(System.getProperty("user.dir")).resolve("build");
        if (!Files.exists(buildDir)) {
            Files.createDirectory(buildDir);
        }
        Path tempDir = buildDir.resolve("temp");
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir);
        }

        return tempDir;
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AccountService accountService(AmazonDynamoDB amazonDynamoDB,
                                         @Value("${dynamodb.accounts}") String accountsTableName,
                                         @Value("${dynamodb.account-mapping}") String accountsMappingTableName,
                                         @Value("${dynamodb.accounts-git}") String accountsGitTableName) {
        return new AccountService(amazonDynamoDB, accountsTableName, accountsMappingTableName, accountsGitTableName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GitServiceFactory gitServiceFactory() {
        return new GitServiceFactory();
    }
}
