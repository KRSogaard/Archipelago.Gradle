package build.archipelago.packageservice.core.data;

import build.archipelago.common.*;
import build.archipelago.common.dynamodb.AV;
import build.archipelago.packageservice.exceptions.*;
import build.archipelago.packageservice.models.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
        log.debug("Finding if build '{}' exists", pkg.toString());
        GetItemRequest request = new GetItemRequest(settings.getPackagesBuildsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.NAME_VERSION, AV.of(this.mergeAccountPackage(accountId, pkg.getNameVersion())))
                        .put(DynamoDBKeys.HASH, AV.of(searchHash(pkg.getHash())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public boolean packageVersionExists(String accountId, ArchipelagoPackage pkg) {
        log.debug("Finding if package and version '{}' exists", pkg.getNameVersion());
        GetItemRequest request = new GetItemRequest(settings.getPackagesVersionsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(this.mergeAccountPackage(accountId, pkg.getName())))
                        .put(DynamoDBKeys.VERSION, AV.of(searchHash(pkg.getVersion())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public boolean packageExists(String accountId, String name) {
        log.debug("Finding if package '{}' exists", name);
        GetItemRequest request = new GetItemRequest(settings.getPackagesTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(name)))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(request).getItem();
        log.debug("Found item: {}", item);
        return item != null;
    }

    @Override
    public PackageDetails getPackageDetails(String accountId, String name) throws PackageNotFoundException {
        log.debug("Getting package details for '{}'", name);
        GetItemRequest request = new GetItemRequest(settings.getPackagesTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(name)))
                        .build());
        Map<String, AttributeValue> pkgItem = dynamoDB.getItem(request).getItem();
        if (pkgItem == null) {
            log.debug("The package '{}' was not found", name);
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
                        AV.of(this.mergeAccountPackage(accountId, name))));
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
            log.debug("Found {} versions for the package '{}'", result.getCount(), name);
        } else {
            log.debug("Did not find any versions for the package '{}'", name);
        }
        return createPackage(pkgItem)
                .versions(versions.build())
                .build();
    }

    @Override
    public ImmutableList<VersionBuildDetails> getPackageVersionBuilds(String accountId, ArchipelagoPackage pkg) {
        log.info("Find all builds for the package '{}'", pkg.getNameVersion());
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
                        AV.of(this.mergeAccountPackage(accountId, pkg.getNameVersion()))));
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
            log.debug("Found {} builds for the package '{}'", result.getCount(), pkg.getNameVersion());
        } else {
            log.debug("Did not find any builds for the package '{}'", pkg.getNameVersion());
        }
        return builds.build();
    }

    @Override
    public BuiltPackageDetails getBuildPackage(String accountId, ArchipelagoBuiltPackage pkg) throws PackageNotFoundException {
        log.debug("Find build '{}'", pkg.toString());
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesBuildsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.NAME_VERSION, AV.of(this.mergeAccountPackage(accountId, pkg.getNameVersion())))
                        .put(DynamoDBKeys.HASH, AV.of(searchHash(pkg.getHash())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Did not find the build '{}'", pkg.toString());
            throw new PackageNotFoundException(pkg);
        }

        return BuiltPackageDetails.builder()
                .hash(item.get(DynamoDBKeys.HASH).getS())
                .config(item.get(DynamoDBKeys.CONFIG).getS())
                .gitCommit(item.get(DynamoDBKeys.GIT_COMMIT).getS())
                .created(AV.toInstant(item.get(DynamoDBKeys.CREATED)))
                .build();
    }

    @Override
    public ArchipelagoBuiltPackage getBuildPackageByGit(String accountId, String packageName, String commit) throws PackageNotFoundException {
        log.debug("Find build '{}' from git commit '{}'", packageName, commit);
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesBuildsGitTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.LOOKUP_KEY, AV.of(this.createGitHash(accountId, packageName, commit)))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Did not find the build '{}' from git commit '{}'", packageName, commit);
            throw new PackageNotFoundException(packageName);
        }

        return new ArchipelagoBuiltPackage(
                item.get(DynamoDBKeys.PACKAGE_NAME).getS(),
                item.get(DynamoDBKeys.VERSION).getS(),
                item.get(DynamoDBKeys.HASH).getS());
    }

    @Override
    public BuiltPackageDetails getLatestBuildPackage(String accountId, ArchipelagoPackage pkg) throws PackageNotFoundException {
        log.debug("Finding the latest build of '{}'", pkg.getNameVersion());
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPackagesVersionsTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(this.mergeAccountPackage(accountId, pkg.getName())))
                        .put(DynamoDBKeys.VERSION, AV.of(searchHash(pkg.getVersion())))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Was unable to find the package '{}'", pkg.getNameVersion());
            throw new PackageNotFoundException(pkg);
        }
        String latestBuild = item.get(DynamoDBKeys.LATEST_BUILD).getS();
        log.debug("Found '{}' as the latest build for '{}'", latestBuild, pkg.getNameVersion());
        return this.getBuildPackage(accountId, new ArchipelagoBuiltPackage(pkg, latestBuild));
    }

    @Override
    public void createBuild(String accountId, ArchipelagoBuiltPackage pkg, String config, String gitCommit) throws
            PackageNotFoundException, PackageExistsException {
        if (!this.packageExists(accountId, pkg.getName())) {
            log.debug("The package '{}' did not exists", pkg.getName());
            throw new PackageNotFoundException(pkg.getName());
        }
        if (this.buildExists(accountId, pkg)) {
            log.debug("The build '{}' already exists", pkg.toString());
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

        if (!this.packageVersionExists(accountId, pkg)) {
            log.debug("The package version '{}' is new", pkg.getNameVersion());
            ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                    .put(DynamoDBKeys.PACKAGE_NAME, AV.of(this.mergeAccountPackage(accountId, pkg.getName())))
                    .put(DynamoDBKeys.VERSION, AV.of(searchVersion(pkg.getVersion())))
                    .put(DynamoDBKeys.DISPLAY_VERSION, AV.of(searchVersion(pkg.getVersion())))
                    .put(DynamoDBKeys.CREATED, AV.of(Instant.now()))
                    .put(DynamoDBKeys.LATEST_BUILD, AV.of(pkg.getHash()))
                    .put(DynamoDBKeys.LATEST_BUILD_TIME, AV.of(now));
            dynamoDB.putItem(new PutItemRequest(settings.getPackagesVersionsTableName(), map.build()));
        } else {
            updateItemRequest = new UpdateItemRequest()
                    .withTableName(settings.getPackagesVersionsTableName())
                    .addKeyEntry(DynamoDBKeys.PACKAGE_NAME, AV.of(this.mergeAccountPackage(accountId, pkg.getName())))
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
                .put(DynamoDBKeys.NAME_VERSION, AV.of(this.mergeAccountPackage(accountId, pkg.getNameVersion())))
                .put(DynamoDBKeys.HASH, AV.of(searchVersion(pkg.getHash())))
                .put(DynamoDBKeys.CREATED, AV.of(now))
                .put(DynamoDBKeys.CONFIG, AV.of(config))
                .put(DynamoDBKeys.GIT_COMMIT, AV.of(gitCommit));
        dynamoDB.putItem(new PutItemRequest(settings.getPackagesBuildsTableName(), map.build()));

        map = ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.LOOKUP_KEY, AV.of(this.createGitHash(accountId, pkg.getName(), gitCommit)))
                .put(DynamoDBKeys.PACKAGE_NAME, AV.of(pkg.getName()))
                .put(DynamoDBKeys.VERSION, AV.of(pkg.getVersion()))
                .put(DynamoDBKeys.HASH, AV.of(searchVersion(pkg.getHash())));
        dynamoDB.putItem(new PutItemRequest(settings.getPackagesBuildsGitTableName(), map.build()));
    }

    private String mergeAccountPackage(String accountId, String packageName) {
        return accountId.toLowerCase() + "_" + packageName.toLowerCase();
    }

    private String createGitHash(String accountId, String packageName, String gitCommit) {
        Preconditions.checkNotNull(packageName);
        Preconditions.checkNotNull(gitCommit);
        String commit = gitCommit;
        if (commit.length() > 7) {
            commit = commit.substring(0, 7);
        }
        return accountId.toLowerCase() + "_" + packageName.toLowerCase() + "_" + commit.toLowerCase();
    }

    @Override
    public void createPackage(String accountId, CreatePackageModel model) throws PackageExistsException {
        log.debug("Create new package: {}", model);
        model.validate();

        if (this.packageExists(accountId, model.getName())) {
            log.debug("The package '{}' already exists", model.getName());
            throw new PackageExistsException(model.getName());
        }

        ImmutableMap.Builder<String, AttributeValue> map = ImmutableMap.<String, AttributeValue>builder()
                .put(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(model.getName())))
                .put(DynamoDBKeys.PACKAGE_PUBLIC, AV.of(model.getPublicPackage()))
                .put(DynamoDBKeys.DISPLAY_PACKAGE_NAME, AV.of(model.getName()))
                .put(DynamoDBKeys.GIT_CLONE_URL, AV.of(model.getGitCloneUrl()))
                .put(DynamoDBKeys.GIT_URL, AV.of(model.getGitUrl()))
                .put(DynamoDBKeys.GIT_REPO_NAME, AV.of(model.getGitRepoName()))
                .put(DynamoDBKeys.GIT_REPO_FULL_NAME, AV.of(model.getGitFullName()))
                .put(DynamoDBKeys.CREATED, AV.of(Instant.now()));
        if (!Strings.isNullOrEmpty(model.getDescription())) {
            map.put(DynamoDBKeys.DESCRIPTION, AV.of(model.getDescription()));
        }
        log.debug("Saving package with attributes: {}", AV.debug(map.build()));

        dynamoDB.putItem(new PutItemRequest(settings.getPackagesTableName(), map.build()));

        if (model.getPublicPackage()) {
            map = ImmutableMap.<String, AttributeValue>builder()
                    .put(DynamoDBKeys.ACCOUNT_ID, AV.of(searchName(accountId)))
                    .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(model.getName())));

            dynamoDB.putItem(new PutItemRequest(settings.getPublicPackagesTableName(), map.build()));
        }
    }

    @Override
    public ImmutableList<PackageDetails> getAllPackages(String accountId) {
        QueryRequest queryRequest = new QueryRequest()
                .withTableName(settings.getPackagesTableName())
                .withKeyConditionExpression("#accountId = :accountId")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#accountId", DynamoDBKeys.ACCOUNT_ID
                ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":accountId", AV.of(searchName(accountId))));

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

            packageDetailsList.add(createPackage(item)
                    .versions(latestVersion.build())
                    .build());
        }
        return packageDetailsList.build();
    }

    @Override
    public String getPublicPackage(String name) throws PackageNotFoundException {
        log.debug("Trying to find public package '{}'", name);
        GetItemRequest getItemRequest = new GetItemRequest(settings.getPublicPackagesTableName(),
                ImmutableMap.<String, AttributeValue>builder()
                        .put(DynamoDBKeys.PACKAGE_NAME, AV.of(searchName(name)))
                        .build());
        Map<String, AttributeValue> item = dynamoDB.getItem(getItemRequest).getItem();
        if (item == null) {
            log.debug("Did not find the public package '{}'", name);
            throw new PackageNotFoundException(name);
        }
        return item.get(DynamoDBKeys.ACCOUNT_ID).getS();
    }

    @Override
    public List<PackageDetails> getAllPublicPackages() {
        ScanRequest request = new ScanRequest()
                .withTableName(settings.getPackagesTableName())
                .withFilterExpression("#pkgPublic = :pkgPublic")
                .withExpressionAttributeNames(ImmutableMap.of(
                        "#pkgPublic", DynamoDBKeys.PACKAGE_PUBLIC
                ))
                .withExpressionAttributeValues(ImmutableMap.of(
                        ":pkgPublic", AV.of(true)));
        ScanResult result = dynamoDB.scan(request);
        if (result.getItems() == null) {
            return new ArrayList<>();
        }

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

            packageDetailsList.add(createPackage(item)
                    .versions(latestVersion.build())
                    .build());
        }
        return packageDetailsList.build();
    }

    private PackageDetails.PackageDetailsBuilder createPackage(Map<String, AttributeValue> item) {
        return PackageDetails.builder()
                .name(item.get(DynamoDBKeys.DISPLAY_PACKAGE_NAME).getS())
                .owner(item.get(DynamoDBKeys.ACCOUNT_ID).getS())
                .publicPackage(AV.getOrDefault(item, DynamoDBKeys.PACKAGE_PUBLIC, AttributeValue::getBOOL, false))
                .description(AV.getStringOrNull(item, DynamoDBKeys.DESCRIPTION))
                .created(AV.toInstant(item.get(DynamoDBKeys.CREATED)))
                .gitCloneUrl(item.get(DynamoDBKeys.GIT_CLONE_URL).getS())
                .gitUrl(item.get(DynamoDBKeys.GIT_URL).getS())
                .gitRepoName(item.get(DynamoDBKeys.GIT_REPO_NAME).getS())
                .gitRepoFullName(item.get(DynamoDBKeys.GIT_REPO_FULL_NAME).getS());
    }
}
