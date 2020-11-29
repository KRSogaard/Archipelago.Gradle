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
                .id(AV.getStringOrNull(item, DynamoDBKeys.ACCOUNT_ID))
                .codeSource(AV.getStringOrNull(item, DynamoDBKeys.CODE_SOURCE))
                .gitHubAccessToken(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCESS_TOKEN))
                .githubAccount(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCOUNT))
                .build();

    }
}
