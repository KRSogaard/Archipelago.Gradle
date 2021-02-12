package build.archipelago.authservice.services.users;

import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.services.*;
import build.archipelago.authservice.services.users.models.*;
import build.archipelago.common.dynamodb.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.*;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.*;

import java.nio.charset.*;
import java.util.*;

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
            throw new UserNotFoundException(email);
        }

        Map<String, AttributeValue> item = result.getItems().stream().findFirst().get();
        String salt = item.get(DBK.PASSWORD_SALT).getS();
        String hashedPassword = hash(password, salt);
        String storedPassword = item.get(DBK.PASSWORD).getS();
        if (!hashedPassword.equals(storedPassword)) {
            log.debug("The password for '{}' did not match", email);
            throw new UserNotFoundException(email);
        }

        return item.get(DBK.USER_ID).getS();
    }

    @Override
    public String createUser(UserModel model) throws UserExistsException {
        QueryRequest queryRequest = new QueryRequest()
                .withIndexName("gsiEmail")
                .withTableName(usersTableName)
                .withKeyConditionExpression("#email = :email")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#email", DBK.EMAIL
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":email", AV.of(model.getEmail())));
        QueryResult result = dynamoDB.query(queryRequest);
        if (result.getItems() != null && result.getItems().size() != 0) {
            throw new UserExistsException(model.getEmail());
        }

        String userId = RandomStringUtils.random(20, true, true);
        String passwordSalt = RandomStringUtils.random(10, true, true);
        String password = hash(model.getPassword(), passwordSalt);

        dynamoDB.putItem(new PutItemRequest(usersTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.USER_ID, AV.of(userId))
                .put(DBK.PASSWORD, AV.of(password))
                .put(DBK.PASSWORD_SALT, AV.of(passwordSalt))
                .put(DBK.NAME, AV.of(model.getName()))
                .put(DBK.EMAIL, AV.of(model.getEmail()))
                .build()));

        return userId;
    }

    private String hash(String password, String salt) {
        return Hashing.sha256()
                .hashString(password + salt, StandardCharsets.UTF_8)
                .toString();
    }
}
