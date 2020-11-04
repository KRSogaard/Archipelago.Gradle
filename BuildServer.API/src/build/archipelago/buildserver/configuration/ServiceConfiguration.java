package build.archipelago.buildserver.configuration;

import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.sqs.AmazonSQS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
@Slf4j
public class ServiceConfiguration {


    public BuildService buildService(AmazonSQS amazonSQS,
                                     AmazonDynamoDB amazonDynamoDB,
                                     @Value("${dynamodb.build.tableName}") String buildTable) {
        return new BuildService(amazonDynamoDB, amazonSQS, buildTable);
    }

    @Bean
    public VersionSetServiceClient versionServiceClient(@Value("services.versionset.url") String vsEndpoint) {
        return new RestVersionSetServiceClient(vsEndpoint);
    }

    @Bean
    public PackageServiceClient packageServiceClient(@Value("services.packages.url") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }
}
