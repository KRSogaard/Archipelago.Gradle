package build.archipelago.buildserver.common.services.build;

import build.archipelago.buildserver.common.services.build.exceptions.BuildRequestNotFoundException;
import build.archipelago.buildserver.common.services.build.models.*;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class BuildService {

    private AmazonDynamoDB dynamoDB;
    private AmazonSQS amazonSQS;
    private String buildTable;
    private String buildQueue;

    public BuildService(AmazonDynamoDB dynamoDB,
                        AmazonSQS amazonSQS,
                        String buildTable,
                        String buildQueue) {
        this.dynamoDB = dynamoDB;
        this.buildTable = buildTable;
        this.buildQueue = buildQueue;
        this.amazonSQS = amazonSQS;
    }

    public String addNewBuildRequest(String versionSet, boolean dryRun, List<BuildPackageDetails> buildPackages) {
        String buildId = UUID.randomUUID().toString();

        dynamoDB.putItem(new PutItemRequest(buildTable, ImmutableMap.<String, AttributeValue>builder()
                .put(Constants.ATTRIBUTE_ID, AV.of(buildId))
                .put(Constants.ATTRIBUTE_CREATED, AV.of(Instant.now()))
                .put(Constants.ATTRIBUTE_VERSION_SET, AV.of(versionSet))
                .put(Constants.ATTRIBUTE_DRY_RUN, AV.of(dryRun))
                .put(Constants.ATTRIBUTE_BUILD_PACKAGES, AV.of(buildPackages.stream()
                        .map(BuildPackageDetails::toString).collect(Collectors.toList())))
                .put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.WAITING.getStatus()))
                .build()));

        amazonSQS.sendMessage(buildQueue, new BuildQueueMessage(buildId).toJson());
        return buildId;
    }

    public BuildRequest getBuildRequest(String buildId) throws BuildRequestNotFoundException {
        GetItemResult result = dynamoDB.getItem(new GetItemRequest(buildTable,
                ImmutableMap.<String, AttributeValue>builder()
                    .put(Constants.ATTRIBUTE_ID, AV.of(buildId))
                .build()));

        if (result.getItem() == null || result.getItem().keySet().size() == 0) {
            throw new BuildRequestNotFoundException(buildId);
        }

        return BuildRequest.builder()
                .buildId(result.getItem().get(Constants.ATTRIBUTE_ID).getS())
                .versionSet(result.getItem().get(Constants.ATTRIBUTE_VERSION_SET).getS())
                .dryRun(result.getItem().get(Constants.ATTRIBUTE_DRY_RUN).getBOOL())
                .buildPackages(result.getItem().get(Constants.ATTRIBUTE_BUILD_PACKAGES).getL().stream()
                        .map(AttributeValue::getS)
                        .map(BuildPackageDetails::parse)
                        .collect(Collectors.toList()))
                .created(AV.toInstant(result.getItem().get(Constants.ATTRIBUTE_CREATED)))
                .build();
    }

    public void setBuildStatus(String buildId, BuildStatus status) {
        GetItemResult itemResult = dynamoDB.getItem(new GetItemRequest(buildTable,ImmutableMap.<String, AttributeValue>builder()
                .put(Constants.ATTRIBUTE_ID, AV.of(buildId))
                .build()));
        if (itemResult.getItem() == null) {
            log.warn("No build was found for build id {}", buildId);
            return;
        }
        Map<String, AttributeValue> item = itemResult.getItem();
        item.put(Constants.ATTRIBUTE_BUILD_STATUS, AV.of(BuildStatus.WAITING.getStatus()));

        dynamoDB.putItem(new PutItemRequest(buildTable, item));
    }
}
