package build.archipelago.versionsetservice.configuration;

import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.core.delegates.*;
import build.archipelago.versionsetservice.core.delegates.addCallback.AddCallbackDelegate;
import build.archipelago.versionsetservice.core.delegates.createVersionSet.CreateVersionSetDelegate;
import build.archipelago.versionsetservice.core.delegates.deleteCallback.DeleteCallbackDelegate;
import build.archipelago.versionsetservice.core.delegates.getCallbacks.GetCallbacksDelegate;
import build.archipelago.versionsetservice.core.services.*;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;

@Configuration
public class VersionSetConfiguration {

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public VersionSetService getVersionSetService(AmazonDynamoDB amazonDynamoDB,
                                                  @Value("${dynamodb.version-sets}") String versionSetTable,
                                                  @Value("${dynamodb.version-sets-revisions}") String versionSetRevisionsTable,
                                                  @Value("${dynamodb.version-sets-callbacks}") String versionSetCallbacksTable
                                                  ) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetRevisionsTable));
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetCallbacksTable));

        return new DynamoDbVersionSetService(amazonDynamoDB, DynamoDbVersionSetServiceConfig.builder()
                .versionSetTable(versionSetTable)
                .versionSetRevisionTable(versionSetRevisionsTable)
                .versionSetCallbacksTable(versionSetCallbacksTable)
                .build());
    }


    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CreateVersionSetDelegate createVersionSetDelegate(VersionSetService versionSetService,
                                                             PackageServiceClient packageServiceClient) {
        return new CreateVersionSetDelegate(versionSetService, packageServiceClient);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public CreateVersionSetRevisionDelegate createVersionSetRevisionDelegate(VersionSetService versionSetService,
                                                                             PackageServiceClient packageServiceClient) {
        return new CreateVersionSetRevisionDelegate(versionSetService, packageServiceClient);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetVersionSetDelegate getVersionSetDelegate(VersionSetService versionSetService) {
        return new GetVersionSetDelegate(versionSetService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetVersionSetPackagesDelegate getVersionSetPackagesDelegate(VersionSetService versionSetService) {
        return new GetVersionSetPackagesDelegate(versionSetService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetVersionSetsDelegate getVersionSetsDelegate(VersionSetService versionSetService) {
        return new GetVersionSetsDelegate(versionSetService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public UpdateVersionSetDelegate getUpdateVersionSetDelegate(VersionSetService versionSetService,
                                                                PackageServiceClient packageServiceClient) {
        return new UpdateVersionSetDelegate(versionSetService, packageServiceClient);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public GetCallbacksDelegate getCallbacksDelegate(VersionSetService versionSetService) {
        return new GetCallbacksDelegate(versionSetService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public DeleteCallbackDelegate getDeleteCallbackDelegate(VersionSetService versionSetService) {
        return new DeleteCallbackDelegate(versionSetService);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AddCallbackDelegate getAddCallbackDelegate(VersionSetService versionSetService) {
        return new AddCallbackDelegate(versionSetService);
    }
}
