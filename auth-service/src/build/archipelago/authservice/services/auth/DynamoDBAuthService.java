package build.archipelago.authservice.services.auth;

import build.archipelago.authservice.models.*;
import build.archipelago.authservice.models.exceptions.*;
import build.archipelago.authservice.services.*;
import build.archipelago.authservice.services.auth.models.*;
import build.archipelago.common.dynamodb.*;
import com.amazonaws.services.dynamodbv2.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.*;
import com.google.common.collect.*;
import lombok.extern.slf4j.*;
import org.apache.commons.lang3.*;

import java.time.*;
import java.util.*;

@Slf4j
public class DynamoDBAuthService implements AuthService {

    private static final String CODE_TYPE_AUTH_CODE = "auth";
    private static final String CODE_TYPE_COOKIE = "cookie";
    private static final String CODE_TYPE_USER_CODE = "user-code";
    private static final String CODE_TYPE_DEVICE_CODE = "device-code";

    private final int authTokenExpiresSec;
    private final int deviceCodeExpiresSec;
    private final int authCookieExpiresSec;
    private final int expiresBufferSec = 10;

    private AmazonDynamoDB dynamoDB;
    private String authCodesTableName;

    public DynamoDBAuthService(AmazonDynamoDB dynamoDB,
                               String authCodesTableName,
                               int authTokenExpiresSec,
                               int deviceCodeExpiresSec,
                               int authCookieExpiresSec) {
        this.dynamoDB = dynamoDB;
        this.authCodesTableName = authCodesTableName;

        this.authTokenExpiresSec = authTokenExpiresSec;
        this.deviceCodeExpiresSec = deviceCodeExpiresSec;
        this.authCookieExpiresSec = authCookieExpiresSec;
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
                .put(DBK.EXPIRES, AV.of(Instant.now().plusSeconds(authTokenExpiresSec + expiresBufferSec)))
                .build()));

        return authCode;
    }

    @Override
    public AuthCodeResult getRequestFromAuthToken(String code) throws TokenNotFoundException, TokenExpiredException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(code))
                .build()));
        Map<String, AttributeValue> item = result.getItem();
        if (item == null || !CODE_TYPE_AUTH_CODE.equalsIgnoreCase(AV.getStringOrNull(item, DBK.CODE_TYPE))) {
            throw new TokenNotFoundException(code);
        }
        if (Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            removeCodeItem(item.get(DBK.AUTH_CODE).getS());
            throw new TokenExpiredException(code);
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
    public String getUserFromAuthCookie(String authCookieToken) throws TokenExpiredException, TokenNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(authCookieToken))
                .build()));
        Map<String, AttributeValue> item = result.getItem();
        if (item == null ||
            !CODE_TYPE_COOKIE.equalsIgnoreCase(AV.getStringOrNull(item, DBK.CODE_TYPE))) {
            throw new TokenNotFoundException(authCookieToken);
        }
        if (Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            removeCodeItem(item.get(DBK.AUTH_CODE).getS());
            throw new TokenExpiredException();
        }

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
                .put(DBK.EXPIRES, AV.of(expires.plusSeconds(expiresBufferSec)))
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
                .put(DBK.CODE_TYPE, AV.of(CODE_TYPE_USER_CODE))
                .put(DBK.DEVICE_CODE, AV.of(deviceCode))
                .put(DBK.CLIENT_ID, AV.of(clientId))
                .put(DBK.SCOPES, AV.of(scope))
                .put(DBK.EXPIRES, AV.of(expires.plusSeconds(expiresBufferSec)))
                .build()));
        dynamoDB.putItem(new PutItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(deviceCode))
                .put(DBK.CODE_TYPE, AV.of(CODE_TYPE_DEVICE_CODE))
                .put(DBK.USER_CODE, AV.of(userCode))
                .put(DBK.EXPIRES, AV.of(expires.plusSeconds(expiresBufferSec)))
                .build()));

        return DeviceCodeResponse.builder()
                .userCode(userCode)
                .deviceCode(deviceCode)
                .expires(expires)
                .build();
    }

    public DeviceCode getDeviceCode(String userCode) throws TokenNotFoundException, TokenExpiredException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(userCode));

        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(userCode))
                .build()));
        Map<String, AttributeValue> item = result.getItem();
        if (item == null ||
            !CODE_TYPE_USER_CODE.equalsIgnoreCase(AV.getStringOrNull(item, DBK.CODE_TYPE))) {
            throw new TokenNotFoundException(userCode);
        }
        if (Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            removeCodeItem(item.get(DBK.AUTH_CODE).getS());
            removeCodeItem(item.get(DBK.DEVICE_CODE).getS());
            throw new TokenExpiredException(userCode);
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
    public DeviceCode getDeviceCodeByDeviceId(String deviceCode) throws TokenNotFoundException, TokenExpiredException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(deviceCode))
                .build()));
        Map<String, AttributeValue> item = result.getItem();
        if (result.getItem() == null ||
            !CODE_TYPE_DEVICE_CODE.equalsIgnoreCase(AV.getStringOrNull(item, DBK.CODE_TYPE))) {
            throw new TokenNotFoundException(deviceCode);
        }
        if (Instant.now().isAfter(AV.toInstant(item.get(DBK.EXPIRES)))) {
            removeCodeItem(item.get(DBK.AUTH_CODE).getS());
            removeCodeItem(item.get(DBK.USER_CODE).getS());
            throw new TokenExpiredException(deviceCode);
        }

        return getDeviceCode(item.get(DBK.USER_CODE).getS());
    }

    public void removeDeviceCode(String userCode) {
        try {
            DeviceCode deviceCode = getDeviceCode(userCode);
            removeCodeItem(deviceCode.getUserCode());
            removeCodeItem(deviceCode.getDeviceCode());
        } catch (TokenNotFoundException | TokenExpiredException e) {
            return;
        }

    }

    @Override
    public void clearExpiredCodes() {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":expires", AV.of(Instant.now()));

        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#expires", DBK.EXPIRES);
        expressionAttributeNames.put("#authcodes", DBK.AUTH_CODE);

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(authCodesTableName)
                .withFilterExpression("#expires < :expires")
                .withProjectionExpression("#authcodes")
                .withExpressionAttributeValues(expressionAttributeValues)
                .withExpressionAttributeNames(expressionAttributeNames);

        List<WriteRequest> deleteRequests = new ArrayList<>();
        ScanResult result = dynamoDB.scan(scanRequest);
        for (Map<String, AttributeValue> item : result.getItems()) {
            deleteRequests.add(new WriteRequest(new DeleteRequest(ImmutableMap.<String, AttributeValue>builder()
                    .put(DBK.AUTH_CODE, AV.of(item.get(DBK.AUTH_CODE).getS()))
                    .build())));
        }

        log.debug("Deleting '{}' expired keys", deleteRequests.size());

        if (deleteRequests.size() > 0) {
            log.debug("Deleting '{}' expired keys", deleteRequests.size());
            BatchWriteItemRequest deleteRequest = new BatchWriteItemRequest();
            deleteRequest.addRequestItemsEntry(authCodesTableName, deleteRequests);
            dynamoDB.batchWriteItem(deleteRequest);
        }
    }

    @Override
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
            put(":userCode", AV.of(userCode.toUpperCase()));
            put(":codeType", AV.of(CODE_TYPE_USER_CODE));
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
                .withConditionExpression("#userCode = :userCode and #codeType = :codeType")
                .withUpdateExpression("set " + String.join(", ", updateExpression))
                .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);
        dynamoDB.updateItem(updateItemRequest);
    }

    private void removeCodeItem(String code) {
        dynamoDB.deleteItem(new DeleteItemRequest(authCodesTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.AUTH_CODE, AV.of(code))
                .build()));
    }
}
