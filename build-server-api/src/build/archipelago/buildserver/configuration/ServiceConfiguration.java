package build.archipelago.buildserver.configuration;

import build.archipelago.buildserver.common.services.build.DynamoDBBuildService;
import build.archipelago.buildserver.common.services.build.logs.*;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DynamoDBBuildService buildService(AmazonSQS amazonSQS,
                                             AmazonDynamoDB amazonDynamoDB,
                                             AmazonS3 amazonS3,
                                             @Value("${dynamodb.build}") String buildTable,
                                             @Value("${dynamodb.build-packages}") String buildPackagesTable,
                                             @Value("${s3.stage-logs}") String bucketNameLogs,
                                             @Value("${sqs.build-queue}") String queueUrl) {
        return new DynamoDBBuildService(amazonDynamoDB, amazonSQS, amazonS3, buildTable, buildPackagesTable, bucketNameLogs, queueUrl);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VersionSetServiceClient versionServiceClient(@Value("${endpoints.versionset-service}") String vsEndpoint,
                                                        @Value("${oauth.client-id}") String clientId,
                                                        @Value("${oauth.client-secret}") String clientSecret) {
        return new RestVersionSetServiceClient(vsEndpoint, clientId, clientSecret);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageServiceClient packageServiceClient(@Value("endpoints.package-service") String pkgEndpoint,
                                                     @Value("${oauth.client-id}") String clientId,
                                                     @Value("${oauth.client-secret}") String clientSecret) {
        return new RestPackageServiceClient(pkgEndpoint, clientId, clientSecret);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageLogsService packageLogsService(AmazonS3 amazonS3,
                                                 @Value("${s3.packages-logs}") String bucketPackageLogs) {
        return new S3PackageLogsService(amazonS3, bucketPackageLogs);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public StageLogsService stageLogsService(AmazonS3 amazonS3,
                                             @Value("${s3.stage-logs}") String bucketStageLogs) {
        return new S3StageLogsService(amazonS3, bucketStageLogs);
    }
}
