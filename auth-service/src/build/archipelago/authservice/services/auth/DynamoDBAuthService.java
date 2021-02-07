package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.AuthorizeRequest;
import build.archipelago.authservice.services.DBK;
import build.archipelago.authservice.services.auth.models.AuthCodeResult;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.util.*;

public class DynamoDBAuthService implements AuthService {

    private static final int authTokenExpiresSec = 60*10;

    private AmazonDynamoDB dynamoDB;
    private String usersTableName;
    private String authCodesTableName;

    public DynamoDBAuthService(AmazonDynamoDB dynamoDB,
                               String usersTableName,
                               String authCodesTableName) {
        this.dynamoDB = dynamoDB;
        this.usersTableName = usersTableName;
        this.authCodesTableName = authCodesTableName;
    }

    @Override
    public String authenticate(String email, String password) {
        return null;
    }

    @Override
    public String getUserFromAuthCode(String authCookieToken) {
        return null;
    }

    @Override
    public String createAuthToken(String userId, AuthorizeRequest request) {
        String authCode = RandomStringUtils.random(32, true, true);

        dynamoDB.putItem(new PutItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(authCode))
                .put(DBK.USER_ID, AV.of(userId))
                .put(DBK.CLIENT_ID, AV.of(request.getClientId()))
                .put(DBK.REDIRECT_URI, AV.of(request.getRedirectUri()))
                .put(DBK.SCOPES, AV.of(request.getScope()))
                .put(DBK.CREATED, AV.of(Instant.now()))
                .put(DBK.EXPIRES, AV.of(Instant.now().plusSeconds(authTokenExpiresSec)))
                .build()));

        return authCode;
    }

    @Override
    public AuthCodeResult getRequestFromAuthToken(String code) {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(code))
                .build()));
        if (result.getItem() == null) {
            return null;
        }

        Map<String, AttributeValue> item = result.getItem();
        return AuthCodeResult.builder()
                .code(item.get(DBK.AUTH_CODE).getS())
                .userId(item.get(DBK.USER_ID).getS())
                .clientId(item.get(DBK.CLIENT_ID).getS())
                .redirectURI(item.get(DBK.REDIRECT_URI).getS())
                .scopes(item.get(DBK.SCOPES).getS())
                .created(AV.toInstant(item.get(DBK.CREATED)))
                .expires(AV.toInstant(item.get(DBK.EXPIRES)))
                .build();
    }
}
