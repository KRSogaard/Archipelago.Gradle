package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.*;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.common.utils.O;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.core.utils.RevisionUtil;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DynamoDbVersionSetService implements VersionSetService {

    private final AmazonDynamoDB dynamoDB;
    private final DynamoDbVersionSetServiceConfig config;

    public DynamoDbVersionSetService(final AmazonDynamoDB dynamoDB,
                                     final DynamoDbVersionSetServiceConfig config) {
        this.dynamoDB = dynamoDB;
        this.config = config;
    }

    @Override
    public List<VersionSet> getAll(String accountId) {
        Preconditions.checkNotNull(accountId);

        QueryRequest request = new QueryRequest()
                .withTableName(config.getVersionSetTable())
                .withKeyConditionExpression("#accountId = :accountId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#accountId", Keys.ACCOUNT_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":accountId", AV.of(this.sanitizeName(accountId))));
        QueryResult result = dynamoDB.query(request);
        List<VersionSet> versionSets = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            versionSets.add(this.parseVersionDBItem(item, new ArrayList<>()));
        }

        return versionSets;
    }

    @Override
    public List<VersionSetCallback> getCallbacks(String accountId, String versionSetName) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(versionSetName);

        QueryRequest request = new QueryRequest()
                .withTableName(config.getVersionSetCallbacksTable())
                .withKeyConditionExpression("#key = :key")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#key", Keys.KEY
                ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":key", AV.of(getKey(accountId, versionSetName))));
        QueryResult result = dynamoDB.query(request);
        List<VersionSetCallback> callbacks = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            callbacks.add(VersionSetCallback.builder()
                    .id(AV.getStringOrNull(item, Keys.ID))
                    .url(AV.getStringOrNull(item, Keys.URL))
                    .build());
        }
        return callbacks;
    }

    @Override
    public void removeCallback(String accountId, String versionSetName, String id) {
        dynamoDB.deleteItem(new DeleteItemRequest(config.getVersionSetCallbacksTable(), ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.KEY, AV.of(this.getKey(accountId, versionSetName)))
                .put(Keys.ID, AV.of(id))
                .build()));
    }

    @Override
    public void addCallback(String accountId, String versionSetName, String url) {
        String id = UUID.randomUUID().toString();
        dynamoDB.putItem(new PutItemRequest(config.getVersionSetCallbacksTable(),  ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.KEY, AV.of(getKey(accountId, versionSetName)))
                .put(Keys.ID, AV.of(id))
                .put(Keys.URL, AV.of(url)).build()));
    }

    @Override
    public void update(String accountId, String versionSetName, UpdateVersionSetRequest request) {
        Map<String, AttributeValue> attributeValues = new HashMap<>();
        Map<String, String> attributeNames = new HashMap<>();
        List<String> updateExpression = new ArrayList<>();

        if (O.isPresent(request.getParent())) {
            attributeNames.put("#parent", Keys.PARENT);
            attributeValues.put(":parent", AV.of(request.getParent().get()));
            updateExpression.add("#parent = :parent");
        }
        if (O.isPresent(request.getTarget())) {
            attributeNames.put("#target", Keys.TARGET);
            attributeValues.put(":target", AV.of(request.getTarget().get().getNameVersion()));
            updateExpression.add("#target = :target");
        }

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(config.getVersionSetTable())
                .addKeyEntry(Keys.ACCOUNT_ID, AV.of(this.sanitizeName(accountId)))
                .addKeyEntry(Keys.VERSION_SET_NAME, AV.of(this.sanitizeName(versionSetName)))
                .withUpdateExpression("set " + String.join(", ", updateExpression))
                .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                .withExpressionAttributeNames(attributeNames)
                .withExpressionAttributeValues(attributeValues);
        dynamoDB.updateItem(updateItemRequest);
    }

    @Override
    public VersionSet get(String accountId, String versionSetName) throws VersionSetDoseNotExistsException {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(versionSetName);

        GetItemResult result = dynamoDB.getItem(new GetItemRequest(config.getVersionSetTable(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(Keys.ACCOUNT_ID, AV.of(this.sanitizeName(accountId)))
                        .put(Keys.VERSION_SET_NAME, AV.of(this.sanitizeName(versionSetName)))
                        .build()));

        if (result.getItem() == null) {
            throw new VersionSetDoseNotExistsException(versionSetName);
        }

        return this.parseVersionDBItem(result.getItem(), this.getRevisions(accountId, versionSetName));
    }

    private VersionSet parseVersionDBItem(Map<String, AttributeValue> dbItem, List<Revision> revisions) {

        ArchipelagoPackage target = null;
        if (dbItem.get(Keys.TARGET) != null) {
            target = ArchipelagoPackage.parse(dbItem.get(Keys.TARGET).getS());
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
        return VersionSet.builder()
                .name(dbItem.get(Keys.DISPLAY_NAME).getS())
                .created(Instant.ofEpochMilli(Long.parseLong(dbItem.get(Keys.CREATED).getN())))
                .updated(dbItem.containsKey(Keys.UPDATED) ? Instant.ofEpochMilli(Long.parseLong(dbItem.get(Keys.UPDATED).getN())) : Instant.now())
                .parent(parent)
                .target(target)
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
                        ":nameKeyValue", AV.of(this.getKey(accountId, versionSetName))));

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
                        .put(Keys.NAME_KEY, AV.of(this.getKey(accountId, versionSetName)))
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
                .target(AV.getOrDefault(result.getItem(), Keys.TARGET, av -> ArchipelagoPackage.parse(av.getS()), null))
                .packages(packages)
                .build();
    }

    @Override
    public void create(String accountId, final String name, ArchipelagoPackage target, String parent, List<String> callbacks) {
        Preconditions.checkNotNull(accountId);
        Preconditions.checkNotNull(name);

        if (target != null && target instanceof ArchipelagoBuiltPackage) {
            target = target.asBase();
        }

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.ACCOUNT_ID, AV.of(this.sanitizeName(accountId)))
                .put(Keys.VERSION_SET_NAME, AV.of(this.sanitizeName(name)))
                .put(Keys.DISPLAY_NAME, AV.of(name))
                .put(Keys.CREATED, AV.of(Instant.now()));

        if (target != null) {
            map.put(Keys.TARGET, AV.of(target.getNameVersion()));
        }
        if (parent != null) {
            map.put(Keys.PARENT, AV.of(this.sanitizeName(parent)));
        }

        dynamoDB.putItem(new PutItemRequest(config.getVersionSetTable(), map.build()));

        // Todo: Do this with bash
        for (String callback : callbacks) {
            String id = UUID.randomUUID().toString();
            dynamoDB.putItem(new PutItemRequest(config.getVersionSetCallbacksTable(),  ImmutableMap.<String, AttributeValue>builder()
                    .put(Keys.KEY, AV.of(getKey(accountId, name)))
                    .put(Keys.ID, AV.of(id))
                    .put(Keys.URL, AV.of(callback)).build()));
        }
    }

    @Override
    public String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages, ArchipelagoPackage target)
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
                    .addKeyEntry(Keys.ACCOUNT_ID, AV.of(this.sanitizeName(accountId)))
                    .addKeyEntry(Keys.VERSION_SET_NAME, AV.of(this.sanitizeName(versionSetName)))
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

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(Keys.NAME_KEY, AV.of(this.getKey(accountId, versionSetName)))
                .put(Keys.REVISION, AV.of(revisionId))
                .put(Keys.CREATED, AV.of(now))
                .put(Keys.PACKAGES, AV.of(
                        packages.stream().map(ArchipelagoBuiltPackage::toString).collect(Collectors.toList())));
        if (target != null) {
            map.put(Keys.TARGET, AV.of(target.getNameVersion()));
        }
        dynamoDB.putItem(new PutItemRequest(config.getVersionSetRevisionTable(), map.build()));

        return revisionId;
    }

    // DynamoDB is case sensitive, the name we get may be entered by a human so we need to sanitize it
    protected String sanitizeName(final String n) {
        return n.trim().toLowerCase();
    }

    private String getKey(String accountId, String name) {
        return this.sanitizeName(accountId) + "_" + this.sanitizeName(name);
    }
}
