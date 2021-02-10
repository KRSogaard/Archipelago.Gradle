package build.archipelago.authservice.services.clients;

import build.archipelago.authservice.services.DBK;
import build.archipelago.authservice.services.clients.eceptions.ClientNotFoundException;
import build.archipelago.authservice.services.clients.models.*;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.util.*;

public class DynamoDBClientService implements ClientService {

    private AmazonDynamoDB dynamoDB;
    private String clientsTableName;

    public DynamoDBClientService(AmazonDynamoDB dynamoDB, String clientsTableName) {
        this.dynamoDB = dynamoDB;
        this.clientsTableName = clientsTableName;
    }

    @Override
    public Client getClient(String clientId) throws ClientNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(clientsTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.CLIENT_ID, AV.of(clientId))
                .build()));
        if (result.getItem() == null) {
            throw new ClientNotFoundException(clientId);
        }

        return parseItem(result.getItem());
    }

    public ClientIdAndSecret createClient(Client client) {
        String clientId = UUID.randomUUID().toString().split("-")[0] +
                UUID.randomUUID().toString().split("-")[0] + UUID.randomUUID().toString().split("-")[0];
        String clientSecret = UUID.randomUUID().toString().split("-")[0] +
                UUID.randomUUID().toString().split("-")[0] + UUID.randomUUID().toString().split("-")[0];

        dynamoDB.putItem(new PutItemRequest(clientsTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.CLIENT_ID, AV.of(clientId))
                .put(DBK.CLIENT_SECRET, AV.of(clientSecret))
                .put(DBK.ALLOWED_SCOPES, AV.of(client.getAllowedScopes()))
                .put(DBK.ALLOWED_REDIRECTS, AV.of(client.getAllowedRedirects()))
                .put(DBK.CREATED, AV.of(Instant.now()))
                .put(DBK.UPDATED, AV.of(Instant.now()))
                .build()));
        return ClientIdAndSecret.builder()
                .id(clientId)
                .secret(clientSecret)
                .build();
    }

    private Client parseItem(Map<String, AttributeValue> item) {
        return Client.builder()
                .clientId(item.get(DBK.CLIENT_ID).getS())
                .clientSecret(AV.getStringOrNull(item, DBK.CLIENT_SECRET))
                .created(AV.toInstant(item.get(DBK.CREATED)))
                .updated(AV.toInstant(item.get(DBK.UPDATED)))
                .allowedRedirects(item.get(DBK.ALLOWED_REDIRECTS).getSS())
                .allowedScopes(item.get(DBK.ALLOWED_SCOPES).getSS())
                .build();
    }
}
