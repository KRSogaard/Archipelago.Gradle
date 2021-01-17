package build.archipelago.packageservice.core.data;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.BuiltPackageDetails;
import build.archipelago.packageservice.models.CreatePackageModel;
import build.archipelago.packageservice.models.PackageDetails;
import build.archipelago.packageservice.models.PackageDetailsVersion;
import build.archipelago.packageservice.models.VersionBuildDetails;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnItemCollectionMetrics;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

@Slf4j
public class DynamoDBPackageData implements PackageData {

    private AmazonDynamoDB dynamoDB;
    private DynamoDBPackageConfig settings;

    public DynamoDBPackageData(AmazonDynamoDB dynamoDB,
                               DynamoDBPackageConfig settings) {
        this.dynamoDB = dynamoDB;
        this.settings = settings;
    }

    private static String searchName(String name) {
        return name.toLowerCase();
    }
    private static String searchVersion(String version) {
        return version.toLowerCase();
    }
    private static String searchHash(String hash) {
        return hash.toLowerCase();
    }

    @Override
    public boolean buildExists(String accountId, ArchipelagoBuiltPackage pkg) {
        log.debug("Finding if build \"{}\" exists", pkg.toString());
        GetItemRequest request = new GetItemRequest(settings.getPackagesBuildsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.NAME_VERSION, AV.of(mergeAccountPackage(accountId, pkg.getNameVersion())))
                        .put(DynamoDBKeys.HASH, AV.of(searchHash(pkg.getHash())))
                    .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public boolean packageVersionExists(String accountId, ArchipelagoPackage pkg) {
        log.debug("Finding if package and version \"{}\" exists", pkg.getNameVersion());
        GetItemRequest request = new GetItemRequest(settings.getPackagesVersionsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(mergeAccountPackage(accountId, pkg.getName())))
                        .put(DynamoDBKeys.VERSION, AV.of(searchHash(pkg.getVersion())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public boolean packageExists(String accountId, String name) {
        log.debug("Finding if package \"{}\" exists", name);
        GetItemRequest request = new GetItemRequest(settings.getPackagesTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(name)))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public PackageDetails getPackageDetails(String accountId, String name) throws PackageNotFoundException {
        log.debug("Getting package details for \"{}\"", name);
        GetItemRequest request = new GetItemRequest(settings.getPackagesTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.ACCOUNT_ID, AV.of(accountId))
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(name)))
                        .build());
        Map<String, AttributeValue> pkgItem = dynamoDB.getItem(request).getItem();
        if (pkgItem == null) {
            log.debug("The package \"{}\" was not found", name);
            throw new PackageNotFoundException(name);
        }

        QueryRequest queryRequest = new QueryRequest()
                .withTableName(settings.getPackagesVersionsTableName())
                .withProjectionExpression("#version, #latestBuild, #latestBuildTime")
                .withKeyConditionExpression("#packageName = :packageName")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#version", DynamoDBKeys.DISPLAY_VERSION,
                        "#latestBuild", DynamoDBKeys.LATEST_BUILD,
                        "#latestBuildTime", DynamoDBKeys.LATEST_BUILD_TIME,
                        "#packageName", DynamoDBKeys.PACKAGE_NAME
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":packageName",
                        AV.of(mergeAccountPackage(accountId, name))));
        QueryResult result = dynamoDB.query(queryRequest);
        ImmutableList.Builder<PackageDetailsVersion> versions = ImmutableList.builder();
        if (result.getItems() != null) {
            result.getItems().forEach(x -> versions.add(
                    PackageDetailsVersion.builder()
                            .version(x.get(DynamoDBKeys.DISPLAY_VERSION).getS())
                            .latestBuildHash(x.get(DynamoDBKeys.LATEST_BUILD).getS())
                            .latestBuildTime(AV.toInstant(x.get(DynamoDBKeys.LATEST_BUILD_TIME)))
                            .build()
            ));
            log.debug("Found {} versions for the package \"{}\"", result.getCount(), name);
        } else {
            log.debug("Did not find any versions for the package \"{}\"", name);
        }
        return PackageDetails.builder()
                .name(pkgItem.get(DynamoDBKeys.DISPLAY_PACKAGE_NAME).getS())
                .created(AV.toInstant(pkgItem.get(DynamoDBKeys.CREATED)))
                .description(pkgItem.get(DynamoDBKeys.DESCRIPTION).getS())
                .gitCloneUrl(pkgItem.get(DynamoDBKeys.GIT_CLONE_URL).getS())
                .gitUrl(pkgItem.get(DynamoDBKeys.GIT_URL).getS())
                .gitRepoName(pkgItem.get(DynamoDBKeys.GIT_REPO_NAME).getS())
                .gitRepoFullName(pkgItem.get(DynamoDBKeys.GIT_REPO_FULL_NAME).getS())
                .versions(versions.build())
                .build();
    }

    @Override
    public ImmutableList<VersionBuildDetails> getPackageVersionBuilds(String accountId, ArchipelagoPackage pkg) {
        log.info("Find all builds for the package \"{}\"", pkg.getNameVersion());
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(settings.getPackagesBuildsTableName())
                .withProjectionExpression("#hash, #created")
                .withKeyConditionExpression("#packageNameVersion = :packageNameVersion")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#hash", DynamoDBKeys.HASH,
                        "#created", DynamoDBKeys.CREATED,
                        "#packageNameVersion", DynamoDBKeys.NAME_VERSION
                ))
                .withExpressionAttributeValues(ImmutableMap.of(":packageNameVersion",
                        AV.of(mergeAccountPackage(accountId, pkg.getNameVersion()))));
        log.debug("Builds query: {}", queryRequest);
        QueryResult result = dynamoDB.query(queryRequest);
        ImmutableList.Builder<VersionBuildDetails> builds = ImmutableList.builder();
        if (result.getItems() != null) {
            result.getItems().forEach(x -> builds.add(
                    VersionBuildDetails.builder()
                            .hash(x.get(DynamoDBKeys.HASH).getS())
                            .created(AV.toInstant(x.get(DynamoDBKeys.CREATED)))
                            .build()
            ));
            log.debug("Found {} builds for the package \"{}\"", result.getCount(), pkg.getNameVersion());
        } else {
            log.debug("Did not find any builds for the package \"{}\"", pkg.getNameVersion());
        }
        return builds.build();
    }

    @Override
    public BuiltPackageDetails getBuildPackage(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        log.debug("Find build \"{}\"", pkg.toString());
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesBuildsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.NAME_VERSION, AV.of(mergeAccountPackage(accountId, pkg.getNameVersion())))
                        .put(DynamoDBKeys.HASH, AV.of(searchHash(pkg.getHash())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Did not find the build \"{}\"", pkg.toString());
            throw new PackageNotFoundException(pkg);
        }

        return BuiltPackageDetails.builder()
                .hash(item.get(DynamoDBKeys.HASH).getS())
                .config(item.get(DynamoDBKeys.CONFIG).getS())
                .gitCommit(item.get(DynamoDBKeys.GIT_COMMIT).getS())
                .gitBranch(item.get(DynamoDBKeys.GIT_BRANCH).getS())
                .created(AV.toInstant(item.get(DynamoDBKeys.CREATED)))
                .build();
    }

    @Override
    public ArchipelagoBuiltPackage getBuildPackageByGit(String accountId, String packageName, String branch, String commit) throws PackageNotFoundException {
        log.debug("Find build \"{}\" from git commit \"{}\", branch \"{}\"", packageName, commit, branch);
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesBuildsGitTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.LOOKUP_KEY, AV.of(createGitHash(accountId, packageName, branch, commit)))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Did not find the build \"{}\" from git commit \"{}\", branch \"{}\"", packageName, commit, branch);
            throw new PackageNotFoundException(packageName);
        }

        return new ArchipelagoBuiltPackage(
                item.get(DynamoDBKeys.PACKAGE_NAME).getS(),
                item.get(DynamoDBKeys.VERSION).getS(),
                item.get(DynamoDBKeys.HASH).getS());
    }

    @Override
    public BuiltPackageDetails getLatestBuildPackage(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException {
        log.debug("Finding the latest build of \"{}\"", pkg.getNameVersion());
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesVersionsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(mergeAccountPackage(accountId, pkg.getName())))
                        .put(DynamoDBKeys.VERSION, AV.of(searchHash(pkg.getVersion())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Was unable to find the package \"{}\"", pkg.getNameVersion());
            throw new PackageNotFoundException(pkg);
        }
        String latestBuild = item.get(DynamoDBKeys.LATEST_BUILD).getS();
        log.debug("Found \"{}\" as the latest build for \"{}\"", latestBuild, pkg.getNameVersion());
        return getBuildPackage(accountId, new ArchipelagoBuiltPackage(pkg, latestBuild));
    }

    @Override
    public void createBuild(String accountId, ArchipelagoBuiltPackage pkg, String config, String gitCommit, String gitBranch) throws
            PackageNotFoundException, PackageExistsException {
        if (!packageExists(accountId, pkg.getName())) {
            log.debug("The package \"{}\" did not exists", pkg.getName());
            throw new PackageNotFoundException(pkg.getName());
        }
        if (buildExists(accountId, pkg)) {
            log.debug("The build \"{}\" already exists", pkg.toString());
            throw new PackageExistsException(pkg);
        }

        Instant now = Instant.now();

        UpdateItemRequest updateItemRequest = new UpdateItemRequest()
                .withTableName(settings.getPackagesTableName())
                .addKeyEntry(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                .addKeyEntry(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(pkg.getName())))
                .withUpdateExpression("set #latestVersion = :latestVersion, " +
                        "#latestBuild = :latestBuild, " +
                        "#latestBuildTime = :latestBuildTime")
                .withConditionExpression("attribute_exists(#packageName)")
                .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#latestBuild", DynamoDBKeys.LATEST_BUILD,
                        "#latestBuildTime", DynamoDBKeys.LATEST_BUILD_TIME,
                        "#latestVersion", DynamoDBKeys.LATEST_VERSION,
                        "#packageName", DynamoDBKeys.PACKAGE_NAME))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":latestBuild", AV.of(pkg.getHash()),
                        ":latestBuildTime", AV.of(now),
                        ":latestVersion", AV.of(pkg.getVersion())));
        dynamoDB.updateItem(updateItemRequest);

        if (!packageVersionExists(accountId, pkg)) {
            log.debug("The package version \"{}\" is new", pkg.getNameVersion());
            ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                    .put(DynamoDBKeys.PACKAGE_NAME, AV.of(mergeAccountPackage(accountId, pkg.getName())))
                    .put(DynamoDBKeys.VERSION, AV.of(searchVersion(pkg.getVersion())))
                    .put(DynamoDBKeys.DISPLAY_VERSION, AV.of(searchVersion(pkg.getVersion())))
                    .put(DynamoDBKeys.CREATED, AV.of(Instant.now()))
                    .put(DynamoDBKeys.LATEST_BUILD, AV.of(pkg.getHash()))
                    .put(DynamoDBKeys.LATEST_BUILD_TIME, AV.of(now));
            dynamoDB.putItem(new PutItemRequest(settings.getPackagesVersionsTableName(), map.build()));
        } else {
            updateItemRequest = new UpdateItemRequest()
                    .withTableName(settings.getPackagesVersionsTableName())
                    .addKeyEntry(DynamoDBKeys.PACKAGE_NAME, AV.of(mergeAccountPackage(accountId, pkg.getName())))
                    .addKeyEntry(DynamoDBKeys.VERSION, AV.of(searchVersion(pkg.getVersion())))
                    .withUpdateExpression("set #latestBuild = :latestBuild, #latestBuildTime = :latestBuildTime")
                    .withConditionExpression("attribute_exists(#packageName)")
                    .withReturnItemCollectionMetrics(ReturnItemCollectionMetrics.SIZE)
                    .withExpressionAttributeNames(ImmutableMap.of(
                            "#latestBuild", DynamoDBKeys.LATEST_BUILD,
                            "#latestBuildTime", DynamoDBKeys.LATEST_BUILD_TIME,
                            "#packageName", DynamoDBKeys.PACKAGE_NAME))
                    .withExpressionAttributeValues(ImmutableMap.of(
                            ":latestBuild", AV.of(pkg.getHash()),
                            ":latestBuildTime", AV.of(now)));
            dynamoDB.updateItem(updateItemRequest);
        }

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.NAME_VERSION, AV.of(mergeAccountPackage(accountId, pkg.getNameVersion())))
                .put(DynamoDBKeys.HASH, AV.of(searchVersion(pkg.getHash())))
                .put(DynamoDBKeys.CREATED, AV.of(now))
                .put(DynamoDBKeys.CONFIG, AV.of(config))
                .put(DynamoDBKeys.GIT_COMMIT, AV.of(gitCommit))
                .put(DynamoDBKeys.GIT_BRANCH, AV.of(gitBranch));
        dynamoDB.putItem(new PutItemRequest(settings.getPackagesBuildsTableName(), map.build()));

        map = ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.LOOKUP_KEY, AV.of(createGitHash(accountId, pkg.getName(), gitBranch, gitCommit)))
                .put(DynamoDBKeys.PACKAGE_NAME, AV.of(pkg.getName()))
                .put(DynamoDBKeys.VERSION, AV.of(pkg.getVersion()))
                .put(DynamoDBKeys.HASH, AV.of(searchVersion(pkg.getHash())));
        dynamoDB.putItem(new PutItemRequest(settings.getPackagesBuildsGitTableName(), map.build()));
    }

    private String mergeAccountPackage(String accountId, String packageName) {
        return accountId.toLowerCase() + "_" + packageName.toLowerCase();
    }

    private String createGitHash(String accountId, String packageName, String gitBranch, String gitCommit) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(gitBranch);
        Preconditions.checkNotNull(gitCommit);
        String commit = gitCommit;
        if (commit.length() > 7) {
            commit = commit.substring(0, 7);
        }
        return accountId.toLowerCase() + "_" + packageName.toLowerCase() + "_" + gitBranch.toLowerCase() + "_" + commit.toLowerCase();
    }

    @Override
    public void createPackage(String accountId, CreatePackageModel model) throws PackageExistsException {
        log.debug("Create new package: {}", model);
        model.validate();

        if (packageExists(accountId, model.getName())) {
            log.debug("The package \"{}\" already exists", model.getName());
            throw new PackageExistsException(model.getName());
        }

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(model.getName())))
                .put(DynamoDBKeys.DISPLAY_PACKAGE_NAME, AV.of(model.getName()))
                .put(DynamoDBKeys.GIT_CLONE_URL, AV.of(model.getGitCloneUrl()))
                .put(DynamoDBKeys.GIT_URL, AV.of(model.getGitUrl()))
                .put(DynamoDBKeys.GIT_REPO_NAME, AV.of(model.getGitRepoName()))
                .put(DynamoDBKeys.GIT_REPO_FULL_NAME, AV.of(model.getGitFullName()))
                .put(DynamoDBKeys.CREATED, AV.of(Instant.now()))
                .put(DynamoDBKeys.DESCRIPTION, AV.of(model.getDescription()));

        dynamoDB.putItem(new PutItemRequest(settings.getPackagesTableName(), map.build()));
    }

    public ImmutableList<PackageDetails> getAllPackages(String accountId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(settings.getPackagesTableName())
                .withKeyConditionExpression("#accountId = :accountId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#accountId", DynamoDBKeys.ACCOUNT_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":accountId", AV.of(accountId)));

        QueryResult result = dynamoDB.query(queryRequest);
        ImmutableList.Builder<PackageDetails> packageDetailsList = ImmutableList.<PackageDetails>builder();
        for (Map<String, AttributeValue> item : result.getItems()) {
            ImmutableList.Builder<PackageDetailsVersion> latestVersion =
                    ImmutableList.<PackageDetailsVersion>builder();

            if (item.containsKey(DynamoDBKeys.LATEST_BUILD)) {
                latestVersion.add(PackageDetailsVersion.builder()
                        .version(item.get(DynamoDBKeys.LATEST_VERSION).getS())
                        .latestBuildHash(item.get(DynamoDBKeys.LATEST_BUILD).getS())
                        .latestBuildTime(AV.toInstant(item.get(DynamoDBKeys.LATEST_BUILD_TIME)))
                        .build());
            }

            packageDetailsList.add(PackageDetails.builder()
                    .name(item.get(DynamoDBKeys.DISPLAY_PACKAGE_NAME).getS())
                    .description(AV.getStringOrNull(item, DynamoDBKeys.DESCRIPTION))
                    .created(AV.toInstant(item.get(DynamoDBKeys.CREATED)))
                    .gitCloneUrl(item.get(DynamoDBKeys.GIT_CLONE_URL).getS())
                    .gitUrl(item.get(DynamoDBKeys.GIT_URL).getS())
                    .gitRepoName(item.get(DynamoDBKeys.GIT_REPO_NAME).getS())
                    .gitRepoFullName(item.get(DynamoDBKeys.GIT_REPO_FULL_NAME).getS())
                    .versions(latestVersion.build())
                    .build());
        }
        return packageDetailsList.build();
    }
}
