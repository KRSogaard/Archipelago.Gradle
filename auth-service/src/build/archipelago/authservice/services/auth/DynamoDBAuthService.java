package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.AuthorizeRequest;
import build.archipelago.authservice.services.DBK;
import build.archipelago.authservice.services.auth.exceptions.DeviceCodeNotFoundException;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.*;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;
import java.util.*;

public class DynamoDBAuthService implements AuthService {

    private static final int authTokenExpiresSec = 60*10;
    private static final int deviceCodeExpiresSec = 60*10;
    private static final int authCookieExpiresSec = 60*60*24*30;
    private static final String CODE_TYPE_AUTH_CODE = "auth";
    private static final String CODE_TYPE_COOKIE = "cookie";
    private static final String CODE_TYPE_DEVICE = "device";

    private AmazonDynamoDB dynamoDB;
    private String authCodesTableName;

    public DynamoDBAuthService(AmazonDynamoDB dynamoDB,
                               String authCodesTableName) {
        this.dynamoDB = dynamoDB;
        this.authCodesTableName = authCodesTableName;
    }

    @Override
    public String createAuthToken(String userId, AuthorizeRequest request) {
        String authCode = RandomStringUtils.random(32, true, true);

        dynamoDB.putItem(new PutItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(authCode))
                .put(DBK.CODE_TYPE, AV.of(CODE_TYPE_AUTH_CODE))
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
        if (!CODE_TYPE_AUTH_CODE.equalsIgnoreCase(item.get(DBK.CODE_TYPE).getS()) ||
                item.get(DBK.EXPIRES) == null ||
                Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            return null;
        }

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

    @Override
    public String getUserFromAuthCookie(String authCookieToken) {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(authCookieToken))
                .build()));
        if (result.getItem() == null) {
            return null;
        }
        Map<String, AttributeValue> item = result.getItem();
        if (!CODE_TYPE_COOKIE.equalsIgnoreCase(item.get(DBK.CODE_TYPE).getS()) ||
            item.get(DBK.EXPIRES) == null ||
            Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            return null;
        }
        return item.get(DBK.USER_ID).getS();
    }

    @Override
    public CodeResponse createAuthCookie(String userId) {
        String authCookieCode = RandomStringUtils.random(32, true, true);
        Instant expires = Instant.now().plusSeconds(authCookieExpiresSec);
        dynamoDB.putItem(new PutItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(authCookieCode))
                .put(DBK.CODE_TYPE, AV.of(CODE_TYPE_COOKIE))
                .put(DBK.USER_ID, AV.of(userId))
                .put(DBK.EXPIRES, AV.of(expires))
                .build()));
        return CodeResponse.builder()
                .code(authCookieCode)
                .expires(expires)
                .build();
    }

    @Override
    public DeviceCodeResponse createDeviceCode(String clientId, String scope) {
        String deviceCode = RandomStringUtils.random(32, true, true);
        String userCode = (
                RandomStringUtils.random(4, true, false)
                + "-" +
                RandomStringUtils.random(4, true, false)
                ).toUpperCase();
        Instant expires = Instant.now().plusSeconds(deviceCodeExpiresSec);

        dynamoDB.putItem(new PutItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(userCode))
                .put(DBK.CODE_TYPE, AV.of(CODE_TYPE_DEVICE))
                .put(DBK.DEVICE_CODE, AV.of(deviceCode))
                .put(DBK.CLIENT_ID, AV.of(clientId))
                .put(DBK.SCOPES, AV.of(scope))
                .put(DBK.EXPIRES, AV.of(expires))
                .build()));
        return DeviceCodeResponse.builder()
                .userCode(userCode)
                .deviceCode(deviceCode)
                .expires(expires)
                .build();
    }

    public DeviceCode getDeviceCode(String userCode) throws DeviceCodeNotFoundException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userCode));

        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(userCode))
                .build()));
        Map<String, AttributeValue> item = result.getItem();
        if (item == null || Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            throw new DeviceCodeNotFoundException(userCode);
        }

        return DeviceCode.builder()
                .userCode(item.get(DBK.AUTH_CODE).getS())
                .deviceCode(item.get(DBK.DEVICE_CODE).getS())
                .clientId(item.get(DBK.CLIENT_ID).getS())
                .expires(AV.toInstant(item.get(DBK.EXPIRES)))
                .scopes(AV.getStringOrNull(item, DBK.SCOPES))
                .updatedAt(AV.toInstantOrNull(item.get(DBK.UPDATED)))
                .userId(AV.getStringOrNull(item, DBK.USER_ID))
                .build();
    }

    public void updateDeviceCode(String userCode, String userId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userCode));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userId));

        Map<String, String> attributeNames = new HashMap<>() {{
            put("#userCode", DBK.AUTH_CODE);
            put("#codeType", DBK.CODE_TYPE);
            put("#userId", DBK.USER_ID);
            put("#updated", DBK.UPDATED);
        }};
        Map<String, AttributeValue> attributeValues = new HashMap<>() {{
            put("#userCode", AV.of(userCode.toUpperCase()));
            put("#codeType", AV.of(CODE_TYPE_DEVICE));
            put(":userId", AV.of(userId));
            put(":updated", AV.of(Instant.now()));
        }};
        List<String> updateExpression = new ArrayList<>() {{
            add("#userId = :userId");
            add("#updated = :updated");
        }};

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(authCodesTableName)
                .addKeyEntry(DBK.AUTH_CODE, AV.of(userCode.toUpperCase()))
                .withConditionExpression("#userCode = :userCode and #codeType = #codeType")
                .withUpdateExpression("set " + String.join(", ", updateExpression))
                .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);
        dynamoDB.updateItem(updateItemRequest);
    }
}
