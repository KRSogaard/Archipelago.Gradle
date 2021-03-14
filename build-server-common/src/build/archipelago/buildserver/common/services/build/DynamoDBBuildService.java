package build.archipelago.buildserver.common.services.build;

import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import build.archipelago.buildserver.models.*;
import build.archipelago.buildserver.models.exceptions.BuildNotFoundException;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.common.utils.Rando;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.base.*;
import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DynamoDBBuildService implements BuildService {

    private AmazonDynamoDB dynamoDB;
    private AmazonSQS amazonSQS;
    private AmazonS3 amazonS3;
    private String buildTable;
    private String buildPackagesTable;
    private String buildLogS3Bucket;
    private String buildQueue;

    public DynamoDBBuildService(AmazonDynamoDB dynamoDB,
                                AmazonSQS amazonSQS,
                                AmazonS3 amazonS3,
                                String buildTable,
                                String buildPackagesTable,
                                String buildLogS3Bucket,
                                String buildQueue) {
        this.dynamoDB = dynamoDB;
        this.buildTable = buildTable;
        this.buildPackagesTable = buildPackagesTable;
        this.buildLogS3Bucket = buildLogS3Bucket;
        this.buildQueue = buildQueue;
        this.amazonSQS = amazonSQS;
        this.amazonS3 = amazonS3;
    }

    @Override
    public String addNewBuildRequest(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> buildPackages) {
        String buildId = Rando.getRandomString();

        dynamoDB.putItem(new PutItemRequest(buildTable, ImmutableMap.<String, AttributeValue>builder()
                .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                .put(Constants.ATTRIBUTE_ACCOUNT_ID, AV.of(accountId))
                .put(Constants.ATTRIBUTE_CREATED, AV.of(Instant.now()))
                .put(Constants.ATTRIBUTE_VERSION_SET, AV.of(versionSet))
                .put(Constants.ATTRIBUTE_DRY_RUN, AV.of(dryRun))
                .put(Constants.ATTRIBUTE_BUILD_PACKAGES, AV.of(buildPackages.stream()
                        .map(BuildPackageDetails::toString).collect(Collectors.toList())))
                .put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.WAITING.getStatus()))
                .put(Constants.ATTRIBUTE_STAGE_PREPARE, AV.of(BuildStatus.WAITING.getStatus()))
                .put(Constants.ATTRIBUTE_STAGE_PACKAGES, AV.of(BuildStatus.WAITING.getStatus()))
                .put(Constants.ATTRIBUTE_STAGE_PUBLISH, AV.of(BuildStatus.WAITING.getStatus()))
                .put(Constants.ATTRIBUTE_UPDATED, AV.of(Instant.now()))
                .build()));

        amazonSQS.sendMessage(buildQueue, BuildQueueMessage.builder()
                .accountId(accountId)
                .buildId(buildId)
                .build().toJson());
        return buildId;
    }

    @Override
    public ArchipelagoBuild getBuildRequest(String accountId, String buildId) throws BuildNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(buildTable,
                ImmutableMap.<String, AttributeValue>builder()
                        .put(Constants.ATTRIBUTE_ACCOUNT_ID, AV.of(accountId))
                        .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                        .build()));

        if (result.getItem() == null || result.getItem().keySet().size() == 0) {
            throw new BuildNotFoundException(buildId);
        }

        return this.parseBuildItem(result.getItem());
    }

    private ArchipelagoBuild parseBuildItem(Map<String, AttributeValue> item) {
        return ArchipelagoBuild.builder()
                .buildId(item.get(Constants.ATTRIBUTE_BUILD_ID).getS())
                .accountId(item.get(Constants.ATTRIBUTE_ACCOUNT_ID).getS())
                .versionSet(item.get(Constants.ATTRIBUTE_VERSION_SET).getS())
                .dryRun(item.get(Constants.ATTRIBUTE_DRY_RUN).getBOOL())
                .buildPackages(item.get(Constants.ATTRIBUTE_BUILD_PACKAGES).getSS().stream()
                        .map(BuildPackageDetails::parse)
                        .collect(Collectors.toList()))
                .created(AV.toInstant(item.get(Constants.ATTRIBUTE_CREATED)))
                .updated(AV.getOrNull(item, Constants.ATTRIBUTE_UPDATED, AV::toInstant))
                .buildStatus(AV.getEnumOrNull(item, Constants.ATTRIBUTE_BUILD_STATUS,
                        (Function<AttributeValue, BuildStatus>) av -> BuildStatus.getEnum(av.getS())))
                .stagePrepare(AV.getEnumOrNull(item, Constants.ATTRIBUTE_STAGE_PREPARE,
                        (Function<AttributeValue, BuildStatus>) av -> BuildStatus.getEnum(av.getS())))
                .stagePackages(AV.getEnumOrNull(item, Constants.ATTRIBUTE_STAGE_PACKAGES,
                        (Function<AttributeValue, BuildStatus>) av -> BuildStatus.getEnum(av.getS())))
                .stagePublish(AV.getEnumOrNull(item, Constants.ATTRIBUTE_STAGE_PUBLISH,
                        (Function<AttributeValue, BuildStatus>) av -> BuildStatus.getEnum(av.getS())))
                .build();
    }

    @Override
    public void setBuildStatus(String accountId, String buildId, BuildStage stage, BuildStatus status) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(stage);
        Preconditions.checkNotNull(status);

        GetItemResult itemResult = dynamoDB.getItem(new GetItemRequest(buildTable, ImmutableMap.<String, AttributeValue>builder()
                .put(Constants.ATTRIBUTE_ACCOUNT_ID, AV.of(accountId))
                .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                .build()));
        if (itemResult.getItem() == null) {
            log.warn("No build was found for build id {}", buildId);
            return;
        }
        Map<String, AttributeValue> item = itemResult.getItem();
        if (BuildStatus.FAILED.equals(status)) {
            item.put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.FAILED.getStatus()));
        } else if ((BuildStatus.FINISHED.equals(status) || BuildStatus.SKIPPED.equals(status)) && BuildStage.PUBLISHING.equals(stage)) {
            item.put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.FINISHED.getStatus()));
        } else {
            item.put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.IN_PROGRESS.getStatus()));
        }
        switch (stage) {
            case PREPARE:
                item.put(Constants.ATTRIBUTE_STAGE_PREPARE, AV.of(status.getStatus()));
                break;
            case PACKAGES:
                item.put(Constants.ATTRIBUTE_STAGE_PACKAGES, AV.of(status.getStatus()));
                break;
            case PUBLISHING:
                item.put(Constants.ATTRIBUTE_STAGE_PUBLISH, AV.of(status.getStatus()));
                break;
        }
        item.put(Constants.ATTRIBUTE_UPDATED, AV.of(Instant.now()));

        dynamoDB.putItem(new PutItemRequest(buildTable, item));
    }

    @Override
    public void setBuildPackages(String buildId, List<PackageBuild> packages) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);
        if (packages.size() > 100) {
            log.info("The package list was larger then 100, we need to do it in batches");
            List<PackageBuild> batch = new ArrayList<>();
            for (int i = 0; i < packages.size(); i++) {
                batch.add(packages.get(i));
                if (batch.size() == 100) {
                    this.executeSetBuildPackages(buildId, batch);
                    batch = new ArrayList<>();
                }
            }
            if (batch.size() > 0) {
                this.executeSetBuildPackages(buildId, batch);
            }
        } else {
            this.executeSetBuildPackages(buildId, packages);
        }
    }

    private void executeSetBuildPackages(String buildId, List<PackageBuild> packages) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        List<WriteRequest> requests = new ArrayList<>();
        for (PackageBuild pkg : packages) {
            requests.add(new WriteRequest().withPutRequest(new PutRequest(ImmutableMap.<String, AttributeValue>builder()
                    .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(this.sanitizeName(buildId)))
                    .put(Constants.ATTRIBUTE_PACKAGE, AV.of(this.sanitizeName(pkg.getPkg().getNameVersion())))
                    .put(Constants.ATTRIBUTE_DIRECT, AV.of(pkg.isDirect()))
                    .put(Constants.ATTRIBUTE_CREATED, AV.of(Instant.now()))
                    .put(Constants.ATTRIBUTE_UPDATED, AV.of(Instant.now()))
                    .put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.WAITING.toString()))
                    .build())));
        }

        BatchWriteItemRequest batchWrite = new BatchWriteItemRequest();
        batchWrite.addRequestItemsEntry(buildPackagesTable, requests);
        BatchWriteItemResult result = dynamoDB.batchWriteItem(batchWrite);
        if (result.getUnprocessedItems().size() > 0) {
            log.error("Failed to store {} build packages", result.getUnprocessedItems().size());
            throw new RuntimeException("Failed to store all build packages");
        }
    }

    @Override
    public void setPackageStatus(String buildId, ArchipelagoPackage pkg, BuildStatus status) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(status);

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(buildPackagesTable)
                .addKeyEntry(Constants.ATTRIBUTE_BUILD_ID, AV.of(this.sanitizeName(buildId)))
                .addKeyEntry(Constants.ATTRIBUTE_PACKAGE, AV.of(this.sanitizeName(pkg.getNameVersion())))
                .withUpdateExpression("set #updated = :updated, " +
                        "#status = :status")
                .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#updated", Constants.ATTRIBUTE_UPDATED,
                        "#status", Constants.ATTRIBUTE_BUILD_STATUS))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":updated", AV.of(Instant.now()),
                        ":status", AV.of(status.getStatus())));
        dynamoDB.updateItem(updateItemRequest);
    }

    private String sanitizeName(String value) {
        return value.toLowerCase();
    }

    @Override
    public List<ArchipelagoBuild> getAllBuildsForAccount(String accountId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(buildTable)
                .withKeyConditionExpression("#accountId = :accountId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#accountId", Constants.ATTRIBUTE_ACCOUNT_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":accountId", AV.of(accountId)));
        QueryResult result = dynamoDB.query(queryRequest);
        List<ArchipelagoBuild> builds = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            builds.add(this.parseBuildItem(item));
        }
        return builds;
    }

    @Override
    public ImmutableList<PackageBuildStatus> getBuildPackages(String accountId, String buildId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(buildPackagesTable)
                .withKeyConditionExpression("#buildId = :buildId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#buildId", Constants.ATTRIBUTE_BUILD_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":buildId", AV.of(buildId)));
        QueryResult result = dynamoDB.query(queryRequest);
        return result.getItems().stream().map(this::parseBuildPackage).collect(ImmutableList.toImmutableList());
    }

    private PackageBuildStatus parseBuildPackage(Map<String, AttributeValue> item) {
        return PackageBuildStatus.builder()
                .pkg(ArchipelagoPackage.parse(item.get(Constants.ATTRIBUTE_PACKAGE).getS()))
                .direct(AV.getOrDefault(item, Constants.ATTRIBUTE_DIRECT, AttributeValue::getBOOL, false))
                .created(AV.toInstant(item.get(Constants.ATTRIBUTE_CREATED)))
                .updated(AV.toInstant(item.get(Constants.ATTRIBUTE_UPDATED)))
                .status(AV.getEnumOrNull(item, Constants.ATTRIBUTE_BUILD_STATUS, (s) -> BuildStatus.getEnum(s.getS())))
                .build();
    }
}
