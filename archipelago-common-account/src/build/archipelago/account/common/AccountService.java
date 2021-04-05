package build.archipelago.account.common;

import build.archipelago.account.common.exceptions.*;
import build.archipelago.account.common.models.*;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.common.git.models.exceptions.GitDetailsNotFound;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class AccountService {
    private AmazonDynamoDB dynamoDB;
    private String accountTableName;
    private String accountGitTableName;
    private String accountMappingTable;

    public AccountService(AmazonDynamoDB dynamoDB, String accountTableName, String accountMappingTable,
                          String accountGitTableName) {
        this.dynamoDB = dynamoDB;
        this.accountTableName = accountTableName;
        this.accountMappingTable = accountMappingTable;
        this.accountGitTableName = accountGitTableName;
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
                .build();
    }

    public ImmutableList<String> getAccountsForUser(String userId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(accountMappingTable)
                .withKeyConditionExpression("#userId = :userId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#userId", DynamoDBKeys.USER_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":userId", AV.of(userId)));
        QueryResult result = dynamoDB.query(queryRequest);

        ImmutableList.Builder<String> list = ImmutableList.<String>builder();
        if (result.getItems() != null && result.getItems().size() != 0) {
            result.getItems().stream().forEach(i -> {
                list.add(i.get(DynamoDBKeys.ACCOUNT_ID).getS());
            });
        }

        return list.build();
    }

    public GitDetails getGitDetails(String accountId) throws GitDetailsNotFound {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(accountGitTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .build()));
        if (result.getItem() == null) {
            throw new GitDetailsNotFound(accountId);
        }

        Map<String, AttributeValue> item = result.getItem();
        return GitDetails.builder()
                .codeSource(AV.getStringOrNull(item, DynamoDBKeys.CODE_SOURCE))
                .githubAccount(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCOUNT))
                .gitHubAccessToken(AV.getStringOrNull(item, DynamoDBKeys.GITHUB_ACCESS_TOKEN))
                .build();
    }

    public void saveGit(String accountId, GitDetails gitDetails) {
        PutItemRequest putItemResult = new PutItemRequest(accountGitTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .put(DynamoDBKeys.CODE_SOURCE, AV.of(gitDetails.getCodeSource()))
                .put(DynamoDBKeys.GITHUB_ACCOUNT, AV.of(gitDetails.getGithubAccount()))
                .put(DynamoDBKeys.GITHUB_ACCESS_TOKEN, AV.of(gitDetails.getGitHubAccessToken()))
                .build());
        dynamoDB.putItem(putItemResult);
    }

    public void createAccount(String accountId) throws AccountExistsException {
        try {
            getAccount(accountId);
            throw new AccountExistsException(accountId);
        } catch (AccountNotFoundException exp) {
            // This is what we want!
        }
        dynamoDB.putItem(new PutItemRequest(accountTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .build()));
    }

    public void attachUserToAccount(String accountId, String userId) {
        if (isUserInAccount(accountId, userId)) {
            return;
        }
        dynamoDB.putItem(new PutItemRequest(accountMappingTable, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.USER_ID, AV.of(userId))
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .build()));
    }

    public boolean isUserInAccount(String accountId, String userId) {
        GetItemResult result = dynamoDB.getItem(accountMappingTable, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(userId))
                .put(DynamoDBKeys.USER_ID, AV.of(accountId))
                .build());
        return result.getItem() != null;
    }

    public Account getAccount(String accountId) throws AccountNotFoundException {
        GetItemResult result = dynamoDB.getItem(accountTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                .build());
        if (result.getItem() == null) {
            throw new AccountNotFoundException(accountId);
        }
        return Account.builder().build();
    }
}
