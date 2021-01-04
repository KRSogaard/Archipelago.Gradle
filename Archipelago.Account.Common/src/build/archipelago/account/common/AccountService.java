package build.archipelago.account.common;

import build.archipelago.account.common.exceptions.AccountNotFoundException;
import build.archipelago.account.common.models.AccountDetails;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class AccountService {
    private AmazonDynamoDB dynamoDB;
    private String accountTableName;
    private String accountMappingTable;
    private Map<String, String> accountMap;

    public AccountService(AmazonDynamoDB dynamoDB, String accountTableName, String accountMappingTable) {
        this.dynamoDB = dynamoDB;
        this.accountTableName = accountTableName;
        this.accountMappingTable = accountMappingTable;
        this.accountMap = new HashMap<>();
    }

    public AccountDetails getAccountDetails(String accountId) throws AccountNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(accountTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .build()));
        if (result.getItem() == null) {
            throw new AccountNotFoundException(accountId);
        }

        Map<String, AttributeValue> item = result.getItem();
        return AccountDetails.builder()
                .id(AV.getStringOrNull(item, DynamoDBKeys.ACCOUNT_ID))
                .codeSource(AV.getStringOrNull(item, DynamoDBKeys.CODE_SOURCE))
                .gitHubAccessToken(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCESS_TOKEN))
                .githubAccount(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCOUNT))
                .build();

    }

    public String getAccountIdForUser(String userId) {
        if (accountMap.containsKey(userId)) {
            return accountMap.get(userId);
        }

        GetItemRequest request = new GetItemRequest(accountMappingTable, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.USER_ID, AV.of(userId))
                .build());
        GetItemResult result = dynamoDB.getItem(request);
        String accountId = UUID.randomUUID().toString().split("-", 2)[0];
        if (result.getItem() != null) {
            accountId = result.getItem().get(DynamoDBKeys.ACCOUNT_ID).getS();
        } else {
            accountId = UUID.randomUUID().toString().split("-", 2)[0];
            dynamoDB.putItem(new PutItemRequest(accountTableName, ImmutableMap.<String, AttributeValue>builder()
                    .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                    .build()));
            dynamoDB.putItem(new PutItemRequest(accountMappingTable, ImmutableMap.<String, AttributeValue>builder()
                    .put(DynamoDBKeys.USER_ID, AV.of(userId))
                    .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                    .build()));
        }
        accountMap.put(userId, accountId);
        return accountId;
    }
}
