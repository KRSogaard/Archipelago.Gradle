package build.archipelago.buildserver.builder.configuration;

import build.archipelago.buildserver.builder.MauiWrapper;
import build.archipelago.buildserver.builder.handlers.*;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.wewelo.sqsconsumer.*;
import com.wewelo.sqsconsumer.models.SQSConsumerConfig;
import com.wewelo.sqsconsumer.threading.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.io.IOException;
import java.nio.file.*;

@Configuration
public class ServiceConfiguration {

    @Bean
    public VersionSetServiceClient versionServiceClient(@Value("${services.versionset.url}") String vsEndpoint) {
        return new RestVersionSetSetServiceClient(vsEndpoint);
    }

    @Bean
    public PackageServiceClient packageServiceClient(@Value("${services.packages.url}") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }

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
    public ExecutorServiceFactory executorServiceFactory() {
        return new BlockingExecutorServiceFactory();
    }

    @Bean
    public MauiWrapper mauiWrapper(@Value("${workspace.maui}") String mauiPath,
                                   @Value("${workspace.path}") String workspacePath) throws IOException {
        Path cachePath = Path.of(workspacePath).resolve("temp");
        if (!Files.exists(cachePath)) {
            Files.createDirectory(cachePath);
        }
        return new MauiWrapper(mauiPath, cachePath);
    }

    @Bean
    public BuildRequestHandler buildRequestHandler(VersionSetServiceClient versionSetServiceClient,
                                                   PackageServiceClient packageServiceClient,
                                                   @Value("${workspace.path}") String workspacePath,
                                                   MauiWrapper mauiWrapper,
                                                   BuildService buildService) throws IOException {
        Path wsPath = Path.of(workspacePath);
        if (!Files.exists(wsPath) || !Files.isDirectory(wsPath)) {
            Files.createDirectory(wsPath);
        }
        return new BuildRequestHandler(versionSetServiceClient, packageServiceClient, wsPath, mauiWrapper, buildService);
    }

    @Bean
    public BuildRequestFailureHandler buildRequestFailureHandler(@Value("${sqs.build-queue}") String buildQueue) {
        return new BuildRequestFailureHandler(buildQueue);
    }

    @Bean
    public SqsMessageProcessorFactory sqsMessageProcessorFactory(BuildRequestHandler buildRequestHandler,
                                                                 BuildRequestFailureHandler buildRequestFailureHandler) {
        return new SqsMessageProcessorFactory(buildRequestHandler, buildRequestFailureHandler);
    }

    @Bean
    public SQSConsumer sqsConsumer(AmazonSQSFactory sqsFactory,
                                   ExecutorServiceFactory executorServiceFactory,
                                   SqsMessageProcessorFactory sqsMessageProcessorFactory,
                                   @Value("${sqs.build-queue}") String buildQueue) {
        SQSConsumerConfig config = new SQSConsumerConfig(buildQueue);
        config.setPollers(1);
        config.setMessageFetchSize(1);
        return new SQSConsumer(sqsFactory, config, sqsMessageProcessorFactory, executorServiceFactory);
    }
}
