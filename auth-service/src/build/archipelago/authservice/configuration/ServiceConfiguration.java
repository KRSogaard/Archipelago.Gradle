package build.archipelago.authservice.configuration;

import build.archipelago.authservice.services.auth.*;
import build.archipelago.authservice.services.clients.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AuthService authService(AmazonDynamoDB dynamoDB,
                                   @Value("${dynamodb.auth-users}") String usersTablesName,
                                   @Value("${dynamodb.auth-codes}") String authCodesTablesName) {
        return new DynamoDBAuthService(dynamoDB, usersTablesName, authCodesTablesName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ClientService clientService(AmazonDynamoDB dynamoDB,
                                     @Value("${dynamodb.auth-clients}") String clientsTableName) {
        return new DynamoDBClientService(dynamoDB, clientsTableName);
    }
}
