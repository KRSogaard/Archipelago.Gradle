package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.Revision;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.versionsetservice.core.utils.RevisionUtil;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnItemCollectionMetrics;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DynamoDbVersionSetService implements VersionSetService {

    private final AmazonDynamoDB dynamoDB;
    private final DynamoDbVersionSetServiceConfig config;

    public DynamoDbVersionSetService(final AmazonDynamoDB dynamoDB,
                                     final DynamoDbVersionSetServiceConfig config) {
        this.dynamoDB = dynamoDB;
        this.config = config;
    }

    @Override
    public VersionSet get(String accountId, String versionSetName) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(versionSetName);

        GetItemResult result = dynamoDB.getItem(new GetItemRequest(config.getVersionSetTable(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(Keys.ACCOUNT_ID, AV.of(sanitizeName(accountId)))
                        .put(Keys.VERSION_SET_NAME, AV.of(sanitizeName(versionSetName)))
                        .build()));

        if (result.getItem() == null) {
            return null;
        }
        Map<String, AttributeValue> dbItem = result.getItem();

        List<ArchipelagoPackage> targets = new ArrayList<>();
        if (dbItem.get(Keys.TARGETS) != null) {
            targets.addAll(dbItem.get(Keys.TARGETS).getSS().stream()
                    .map(ArchipelagoPackage::parse).collect(Collectors.toList()));
        }

        String parent = null;
        if (dbItem.get(Keys.PARENT) != null) {
            parent = dbItem.get(Keys.PARENT).getS();
        }

        String latestRevision = null;
        Instant latestRevisionCreated = null;
        if (dbItem.get(Keys.REVISION_LATEST) != null) {
            latestRevision = dbItem.get(Keys.REVISION_LATEST).getS();
            latestRevisionCreated = AV.toInstant(dbItem.get(Keys.REVISION_CREATED));
        }

        List<Revision> revisions = getRevisions(accountId, versionSetName);

        return VersionSet.builder()
                .name(dbItem.get(Keys.DISPLAY_NAME).getS())
                .created(Instant.ofEpochMilli(Long.parseLong(dbItem.get(Keys.CREATED).getN())))
                .parent(parent)
                .targets(targets)
                .revisions(revisions)
                .latestRevision(latestRevision)
                .latestRevisionCreated(latestRevisionCreated)
                .build();
    }

    private List<Revision> getRevisions(String accountId, String versionSetName) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(versionSetName);

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(config.getVersionSetRevisionTable())
                .withProjectionExpression("#revision, #created")
                .withKeyConditionExpression("#nameKey = :nameKeyValue")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#nameKey", Keys.NAME_KEY,
                        "#revision", Keys.REVISION,
                        "#created", Keys.CREATED
                        ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":nameKeyValue", AV.of(getKey(accountId, versionSetName))));

        QueryResult result = dynamoDB.query(queryRequest);
        if (result.getItems() == null || result.getItems().size() == 0) {
            return new ArrayList<>();
        }
        return result.getItems().stream().map(x -> Revision.builder()
            .revisionId(x.get(Keys.REVISION).getS())
            .created(AV.toInstant(x.get(Keys.CREATED)))
            .build()).collect(Collectors.toList());
    }

    @Override
    public VersionSetRevision getRevision(String accountId, String versionSetName, String revision) throws VersionSetDoseNotExistsException {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(versionSetName);
        Preconditions.checkNotNull(revision);

        GetItemResult result = dynamoDB.getItem(new GetItemRequest(config.getVersionSetRevisionTable(),
                ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.NAME_KEY, AV.of(getKey(accountId, versionSetName)))
                .put(Keys.REVISION, AV.of(revision.toLowerCase()))
                .build()));
        if (result.getItem() == null) {
            throw new VersionSetDoseNotExistsException(versionSetName, revision);
        }

        List<ArchipelagoBuiltPackage> packages = new ArrayList<>();
        // TODO: This is a bit hacky, clean this up
        if (result.getItem().get(Keys.PACKAGES) != null &&
            result.getItem().get(Keys.PACKAGES).getSS() != null &&
            result.getItem().get(Keys.PACKAGES).getSS().size() > 0) {
            packages.addAll(result.getItem().get(Keys.PACKAGES).getSS().stream()
                    .map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList()));
        }

        return VersionSetRevision.builder()
                .created(AV.toInstant(result.getItem().get(Keys.CREATED)))
                .packages(packages)
                .build();
    }

    @Override
    public void create(String accountId, final String name, final List<ArchipelagoPackage> targets, final Optional<String> parent) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(targets);

        for (ArchipelagoPackage t : targets) {
            if (t instanceof ArchipelagoBuiltPackage) {
                throw new IllegalArgumentException(t.toString() + " can not have a build version");
            }
        }

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.ACCOUNT_ID, AV.of(sanitizeName(accountId)))
                .put(Keys.VERSION_SET_NAME, AV.of(sanitizeName(name)))
                .put(Keys.DISPLAY_NAME, AV.of(name))
                .put(Keys.CREATED, AV.of(Instant.now()))
                .put(Keys.TARGETS,
                    AV.of(targets.stream().map(x -> ((ArchipelagoPackage)x).toString()).collect(Collectors.toList())));
        if (parent != null && parent.isPresent()) {
            map.put(Keys.PARENT, AV.of(sanitizeName(parent.get())));
        }

        dynamoDB.putItem(new PutItemRequest(config.getVersionSetTable(), map.build()));
    }

    @Override
    public String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages)
            throws VersionSetDoseNotExistsException {
        Preconditions.checkNotNull(versionSetName);
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        String revisionId = RevisionUtil.getRandomRevisionId();

        Instant now = Instant.now();

        // Update the latest revision table
        try {
            UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                    .withTableName(config.getVersionSetTable())
                    .addKeyEntry(Keys.ACCOUNT_ID, AV.of(sanitizeName(accountId)))
                    .addKeyEntry(Keys.VERSION_SET_NAME, AV.of(sanitizeName(versionSetName)))
                    .withUpdateExpression("set #revisionLatest = :revisionLatest, #revisionCreated = :revisionCreated")
                    .withConditionExpression("(attribute_exists(#name) and attribute_exists(#accountId))")
                    .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                    .withExpressionAttributeNames(ImmutableMap.of(
                            "#revisionLatest", Keys.REVISION_LATEST,
                            "#revisionCreated", Keys.REVISION_CREATED,
                            "#name", Keys.VERSION_SET_NAME,
                            "#accountId", Keys.ACCOUNT_ID))
                    .withExpressionAttributeValues(ImmutableMap.of(
                            ":revisionLatest", AV.of(revisionId),
                            ":revisionCreated", AV.of(now)));
            dynamoDB.updateItem(updateItemRequest);
        } catch (ConditionalCheckFailedException exp) {
            throw new VersionSetDoseNotExistsException(versionSetName, exp);
        }


        dynamoDB.putItem(new PutItemRequest(config.getVersionSetRevisionTable(), ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.NAME_KEY, AV.of(getKey(accountId, versionSetName)))
                .put(Keys.REVISION, AV.of(revisionId))
                .put(Keys.CREATED, AV.of(now))
                .put(Keys.PACKAGES, AV.of(
                        packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList())))
                .build()));


        return revisionId;
    }

    // DynamoDB is case sensitive, the name we get may be entered by a human so we need to sanitize it
    protected String sanitizeName(final String n) {
        return n.trim().toLowerCase();
    }
    private String getKey(String accountId, String name) {
        return sanitizeName(accountId)+"_"+ sanitizeName(name);
    }
}
