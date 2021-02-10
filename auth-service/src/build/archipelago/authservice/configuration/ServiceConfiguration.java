package build.archipelago.authservice.configuration;

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
}
