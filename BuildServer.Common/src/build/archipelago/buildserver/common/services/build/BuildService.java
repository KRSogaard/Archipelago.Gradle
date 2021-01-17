package build.archipelago.buildserver.common.services.build;

import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import build.archipelago.buildserver.common.services.build.models.PackageBuild;
import build.archipelago.buildserver.models.ArchipelagoBuild;
import build.archipelago.buildserver.models.BuildPackageDetails;
import build.archipelago.buildserver.models.BuildStage;
import build.archipelago.buildserver.models.BuildStatus;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class BuildService {

    private AmazonDynamoDB dynamoDB;
    private AmazonSQS amazonSQS;
    private AmazonS3 amazonS3;
    private String buildTable;
    private String buildPackagesTable;
    private String buildLogS3Bucket;
    private String buildQueue;

    public BuildService(AmazonDynamoDB dynamoDB,
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

    public String addNewBuildRequest(String accountId, String versionSet, boolean dryRun, List<BuildPackageDetails> buildPackages) {
        String buildId = UUID.randomUUID().toString();

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

    public ArchipelagoBuild getBuildRequest(String accountId, String buildId) throws BuildRequestNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(buildTable,
                ImmutableMap.<String, AttributeValue>builder()
                    .put(Constants.ATTRIBUTE_ACCOUNT_ID, AV.of(accountId))
                    .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                .build()));

        if (result.getItem() == null || result.getItem().keySet().size() == 0) {
            throw new BuildRequestNotFoundException(buildId);
        }

        return parseBuildItem(result.getItem());
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

    public void setBuildStatus(String accountId, String buildId, BuildStage stage, BuildStatus status) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(stage);
        Preconditions.checkNotNull(status);

        GetItemResult itemResult = dynamoDB.getItem(new GetItemRequest(buildTable,ImmutableMap.<String, AttributeValue>builder()
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
        } else if (BuildStatus.FINISHED.equals(status) && BuildStage.PUBLISHING.equals(stage)) {
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
                    executeSetBuildPackages(buildId, batch);
                    batch = new ArrayList<>();
                }
            }
            if (batch.size() > 0) {
                executeSetBuildPackages(buildId, batch);
            }
        } else {
            executeSetBuildPackages(buildId, packages);
        }
    }
    private void executeSetBuildPackages(String buildId, List<PackageBuild> packages) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(packages);
        Preconditions.checkArgument(packages.size() > 0);

        List<WriteRequest> requests = new ArrayList<>();
        for(PackageBuild pkg : packages) {
            requests.add(new WriteRequest().withPutRequest(new PutRequest(ImmutableMap.<String, AttributeValue>builder()
                    .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                    .put(Constants.ATTRIBUTE_PACKAGE, AV.of(pkg.getPkg().getNameVersion()))
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

    public void setPackageStatus(String buildId, ArchipelagoPackage pkg, BuildStatus status) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(status);


        dynamoDB.putItem(new PutItemRequest(buildPackagesTable, ImmutableMap.<String, AttributeValue>builder()
                .put(Constants.ATTRIBUTE_BUILD_ID, AV.of(buildId))
                .put(Constants.ATTRIBUTE_PACKAGE, AV.of(pkg.getNameVersion()))
                .put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(status.getStatus()))
                .put(Constants.ATTRIBUTE_UPDATED, AV.of(Instant.now()))
                .build()));
    }

    public void uploadStageLog(String buildId, BuildStage buildStage, String readString) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(buildId));
        Preconditions.checkNotNull(buildStage);
        Preconditions.checkArgument(!Strings.isNullOrEmpty(readString));

        amazonS3.putObject(buildLogS3Bucket, getS3LogKey(buildId, buildStage.getStage()), readString);
    }

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
            builds.add(parseBuildItem(item));
        }
        return builds;
    }

    private String getS3LogKey(String buildId, String nameVersion) {
        return buildId.toLowerCase() + "/" + nameVersion.toLowerCase() + ".log";
    }
}
