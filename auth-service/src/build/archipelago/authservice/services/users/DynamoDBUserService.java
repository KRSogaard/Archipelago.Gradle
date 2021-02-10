package build.archipelago.authservice.services.users;

import build.archipelago.authservice.services.DBK;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;
import build.archipelago.authservice.services.users.exceptions.*;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class DynamoDBUserService implements UserService {

    private AmazonDynamoDB dynamoDB;
    private String usersTableName;

    public DynamoDBUserService(AmazonDynamoDB dynamoDB,
                               String usersTableName) {
        this.dynamoDB = dynamoDB;
        this.usersTableName = usersTableName;
    }

    @Override
    public String authenticate(String email, String password) throws UserNotFoundException {
        QueryRequest queryRequest = new QueryRequest()
                .withIndexName("gsiEmail")
                .withTableName(usersTableName)
                .withKeyConditionExpression("#email = :email")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#email", DBK.EMAIL
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":email", AV.of(email)));
        QueryResult result = dynamoDB.query(queryRequest);
        if (result.getItems() == null || result.getItems().size() == 0) {
            log.debug("No user with the email '{}' was found", email);
            throw new UserNotFoundException();
        }

        Map<String, AttributeValue> item = result.getItems().stream().findFirst().get();
        String salt = item.get(DBK.PASSWORD_SALT).getS();
        String hashedPassword = hash(password, salt);
        String storedPassword = item.get(DBK.PASSWORD).getS();
        if (!hashedPassword.equals(storedPassword)) {
            log.debug("The password for '{}' did not match", email);
            throw new UserNotFoundException();
        }

        return item.get(DBK.USER_ID).getS();
    }

    private String hash(String password, String salt) {
        return Hashing.sha256()
                .hashString(password + salt, StandardCharsets.UTF_8)
                .toString();
    }
}
