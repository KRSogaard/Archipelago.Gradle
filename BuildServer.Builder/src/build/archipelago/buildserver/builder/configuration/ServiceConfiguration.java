package build.archipelago.buildserver.builder.configuration;

import build.archipelago.account.common.AccountService;
import build.archipelago.buildserver.builder.builder.BuilderFactory;
import build.archipelago.buildserver.builder.clients.InternalHarborClientFactory;
import build.archipelago.buildserver.builder.handlers.BuildRequestFailureHandler;
import build.archipelago.buildserver.builder.handlers.BuildRequestHandler;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.maui.graph.DependencyGraphGenerator;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.BinRecipe;
import build.archipelago.maui.path.recipies.PackageRecipe;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.rest.RestPackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import build.archipelago.versionsetservice.client.rest.RestVersionSetServiceClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.wewelo.sqsconsumer.AmazonSQSFactory;
import com.wewelo.sqsconsumer.SQSConsumer;
import com.wewelo.sqsconsumer.SqsMessageProcessorFactory;
import com.wewelo.sqsconsumer.models.SQSConsumerConfig;
import com.wewelo.sqsconsumer.threading.BlockingExecutorServiceFactory;
import com.wewelo.sqsconsumer.threading.ExecutorServiceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class ServiceConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VersionSetServiceClient versionServiceClient(@Value("${services.versionset.url}") String vsEndpoint,
                                                        @Value("${oauth.client-id}") String clientId,
                                                        @Value("${oauth.client-secret}") String clientSecret) {
        return new RestVersionSetServiceClient(vsEndpoint, clientId, clientSecret);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public PackageServiceClient packageServiceClient(@Value("${services.packages.url}") String pkgEndpoint,
                                                     @Value("${oauth.client-id}") String clientId,
                                                     @Value("${oauth.client-secret}") String clientSecret) {
        return new RestPackageServiceClient(pkgEndpoint, clientId, clientSecret);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AccountService accountService(AmazonDynamoDB amazonDynamoDB,
                                         @Value("${dynamodb.accounts}") String accountsTableName,
                                         @Value("${dynamodb.account-mapping}") String accountsMappingTableName) {
        return new AccountService(amazonDynamoDB, accountsTableName, accountsMappingTableName);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
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
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ExecutorServiceFactory executorServiceFactory() {
        return new BlockingExecutorServiceFactory();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public BuilderFactory builderFactory(InternalHarborClientFactory internalHarborClientFactory,
                                         VersionSetServiceClient versionSetServiceClient,
                                         PackageServiceClient packageServiceClient,
                                         BuildService buildService,
                                         AccountService accountService,
                                         MauiPath mauiPath,
                                         @Value("${workspace.path}") String workspacePath) throws IOException {
        Path wsPath = Path.of(workspacePath);
        if (!Files.exists(wsPath) || !Files.isDirectory(wsPath)) {
            Files.createDirectory(wsPath);
        }
        return new BuilderFactory(internalHarborClientFactory, versionSetServiceClient,
                packageServiceClient, wsPath,
                buildService, accountService,
                mauiPath);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public BuildRequestHandler buildRequestHandler(BuilderFactory builderFactory) {
        return new BuildRequestHandler(builderFactory);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public BuildRequestFailureHandler buildRequestFailureHandler(@Value("${sqs.build-queue}") String buildQueue) {
        return new BuildRequestFailureHandler(buildQueue);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SqsMessageProcessorFactory sqsMessageProcessorFactory(BuildRequestHandler buildRequestHandler,
                                                                 BuildRequestFailureHandler buildRequestFailureHandler) {
        return new SqsMessageProcessorFactory(buildRequestHandler, buildRequestFailureHandler);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SQSConsumer sqsConsumer(AmazonSQSFactory sqsFactory,
                                   ExecutorServiceFactory executorServiceFactory,
                                   SqsMessageProcessorFactory sqsMessageProcessorFactory,
                                   @Value("${sqs.build-queue}") String buildQueue) {
        SQSConsumerConfig config = new SQSConsumerConfig(buildQueue);
        config.setPollers(1);
        config.setMessageFetchSize(1);
        return new SQSConsumer(sqsFactory, config, sqsMessageProcessorFactory, executorServiceFactory);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public InternalHarborClientFactory internalHarborClientFactory(
            VersionSetServiceClient versionSetServiceClient,
            PackageServiceClient packageServiceClient) {
        return new InternalHarborClientFactory(versionSetServiceClient, packageServiceClient);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DependencyGraphGenerator dependencyGraphGenerator() {
        return new DependencyGraphGenerator();
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public MauiPath mauiPath(DependencyGraphGenerator dependencyGraphGenerator) {
        return new MauiPath(List.of(
                new BinRecipe(),
                new PackageRecipe()
        ), dependencyGraphGenerator);
    }
}
