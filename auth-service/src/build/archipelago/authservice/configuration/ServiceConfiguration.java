package build.archipelago.authservice.configuration;

import build.archipelago.account.common.AccountService;
import build.archipelago.authservice.services.accessKeys.AccessKeyService;
import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.clients.*;
import build.archipelago.authservice.services.keys.*;
import build.archipelago.authservice.services.users.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AuthService authService(AmazonDynamoDB dynamoDB,
                                   @Value("${dynamodb.auth-codes}") String authCodesTablesName,
                                   @Value("${token.age.authToken}") int authTokenExpiresSec,
                                   @Value("${token.age.deviceCode}") int deviceCodeExpiresSec,
                                   @Value("${token.age.authCookie}") int authCookieExpiresSec) {
        return new DynamoDBAuthService(dynamoDB, authCodesTablesName, authTokenExpiresSec, deviceCodeExpiresSec, authCookieExpiresSec);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public UserService userService(AmazonDynamoDB dynamoDB,
                                   @Value("${dynamodb.auth-users}") String usersTablesName) {
        return new DynamoDBUserService(dynamoDB, usersTablesName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ClientService clientService(AmazonDynamoDB dynamoDB,
                                     @Value("${dynamodb.auth-clients}") String clientsTableName) {
        return new DynamoDBClientService(dynamoDB, clientsTableName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public KeyService keyService(AmazonDynamoDB dynamoDB,
                                 @Value("${dynamodb.auth-jwks}") String keysTableName) {
        return new DynamoDBKeyService(dynamoDB, keysTableName);
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
    public AccessKeyService accessKeyService(AmazonDynamoDB amazonDynamoDB,
                                             @Value("${dynamodb.access-keys}") String accessKeysTablesName) {
        return new AccessKeyService(amazonDynamoDB, accessKeysTablesName);
    }
}
