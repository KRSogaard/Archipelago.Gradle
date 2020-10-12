package build.archipelago.buildserver.builder.configuration;

import build.archipelago.buildserver.builder.handlers.*;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.xspec.B;
import com.amazonaws.services.sqs.AmazonSQS;
import com.wewelo.sqsconsumer.*;
import com.wewelo.sqsconsumer.models.SQSConsumerConfig;
import com.wewelo.sqsconsumer.threading.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class ServiceConfiguration {

    @Bean
    public VersionSetServiceClient versionServiceClient(@Value("services.versionset.url") String vsEndpoint) {
        return new RestVersionSetSetServiceClient(vsEndpoint);
    }

    @Bean
    public PackageServiceClient packageServiceClient(@Value("services.packages.url") String pkgEndpoint) {
        return new RestPackageServiceClient(pkgEndpoint);
    }

    @Bean
    public BuildService buildService(AmazonSQS amazonSQS,
                                     AmazonDynamoDB amazonDynamoDB,
                                     @Value("${dynamodb.build.tableName}") String buildTable,
                                     @Value("${sqs.build-queue}") String queueUrl) {
        return new BuildService(amazonDynamoDB, amazonSQS, buildTable, queueUrl);
    }

    @Bean
    public ExecutorServiceFactory executorServiceFactory() {
        return new BlockingExecutorServiceFactory();
    }

    @Bean
    public BuildRequestHandler buildRequestHandler() {
        return new BuildRequestHandler();
    }

    @Bean
    public BuildRequestFailureHandler buildRequestFailureHandler() {
        return new BuildRequestFailureHandler();
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
