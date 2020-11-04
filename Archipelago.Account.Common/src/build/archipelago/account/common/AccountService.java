package build.archipelago.account.common;

import build.archipelago.account.common.exceptions.AccountNotFoundException;
import build.archipelago.account.common.models.AccountDetails;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class AccountService {
    private AmazonDynamoDB dynamoDB;
    private String accountTableName;

    public AccountService(AmazonDynamoDB dynamoDB, String accountTableName) {
        this.dynamoDB = dynamoDB;
        this.accountTableName = accountTableName;
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
                .name(AV.getStringOrNull(item, DynamoDBKeys.NAME))
                .codeSource(AV.getStringOrNull(item, DynamoDBKeys.CODE_SOURCE))
                .gitHubApi(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_API))
                .gitHubClientId(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_CLIENT_ID))
                .gitHubClientSecret(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_CLIENT_SECRET))
                .githubCode(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_CODE))
                .gitHubRepo(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_REPO))
                .build();

    }
}
