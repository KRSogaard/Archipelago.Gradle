package build.archipelago.packageservice.configuration;

import build.archipelago.account.common.AccountService;
import build.archipelago.common.github.GitServiceFactory;
import build.archipelago.packageservice.core.data.*;
import build.archipelago.packageservice.core.storage.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageStorage packageStorage(AmazonS3 amazonS3,
                                         @Value("${s3.packages}") String bucketName) {
        log.info("Creating S3PackageStorage using bucket '{}'",
                bucketName);
        return new S3PackageStorage(amazonS3, bucketName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageData packageData(
            @Value("${dynamodb.packages}") String packageTable,
            @Value("${dynamodb.packages_public}") String packagePublicTable,
            @Value("${dynamodb.packages_versions}") String packageVersionsTable,
            @Value("${dynamodb.packages_builds}") String packageBuildsTable,
            @Value("${dynamodb.packages_builds_git}") String packageBuildsGitTable,
            AmazonDynamoDB dynamoDB) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packagePublicTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageVersionsTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageBuildsTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(packageBuildsGitTable));

        DynamoDBPackageConfig config = DynamoDBPackageConfig.builder()
                .packagesTableName(packageTable)
                .publicPackagesTableName(packagePublicTable)
                .packagesVersionsTableName(packageVersionsTable)
                .packagesBuildsTableName(packageBuildsTable)
                .packagesBuildsGitTableName(packageBuildsGitTable)
                .build();

        log.info("Creating DynamoDBPackageData with config '{}'",
                config.toString());
        return new DynamoDBPackageData(dynamoDB, config);
    }


    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AccountService accountService(AmazonDynamoDB amazonDynamoDB,
                                         @Value("${dynamodb.accounts}") String accountsTableName,
                                         @Value("${dynamodb.account-mapping}") String accountsMappingTableName,
                                         @Value("${dynamodb.accounts-git}") String accountsGitTableName) {
        return new AccountService(amazonDynamoDB, accountsTableName, accountsMappingTableName, accountsGitTableName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GitServiceFactory gitServiceFactory() {
        return new GitServiceFactory();
    }
}
