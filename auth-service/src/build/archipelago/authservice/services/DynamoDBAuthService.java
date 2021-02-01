package build.archipelago.authservice.services;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

public class DynamoDBAuthService implements AuthService {

    private AmazonDynamoDB dynamoDB;
    private String usersTableName;

    public DynamoDBAuthService(AmazonDynamoDB dynamoDB, String usersTableName) {
        this.dynamoDB = dynamoDB;
        this.usersTableName = usersTableName;
    }

    @Override
    public String authenticate(String email, String password) {
        return null;
    }
}
