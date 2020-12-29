package build.archipelago.buildserver.configuration;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class ServiceConfiguration {


    @Bean
    public BuildService buildService(AmazonSQS amazonSQS,
                                     AmazonDynamoDB amazonDynamoDB,
                                     AmazonS3 amazonS3,
                                     @Value("${dynamodb.build}") String buildTable,
                                     @Value("${dynamodb.build-packages}") String buildPackagesTable,
                                     @Value("${s3.logs}") String bucketNameLogs,
                                     @Value("${sqs.build-queue}") String queueUrl) {
        return new BuildService(amazonDynamoDB, amazonSQS, amazonS3, buildTable, buildPackagesTable, bucketNameLogs, queueUrl);
    }

    @Bean
    public VersionSetServiceClient versionServiceClient(@Value("${services.versionset.url}") String vsEndpoint,
                                                        @Value("${oauth.client-id}") String clientId,
                                                        @Value("${oauth.client-secret}") String clientSecret) {
        return new RestVersionSetServiceClient(vsEndpoint, clientId, clientSecret);
    }
//
//    @Bean
//    public PackageServiceClient packageServiceClient(@Value("services.packages.url") String pkgEndpoint) {
//        return new RestPackageServiceClient(pkgEndpoint);
//    }
}
