package build.archipelago.buildserver.builder.handlers;

import build.archipelago.account.common.AccountService;
import build.archipelago.buildserver.builder.MauiWrapper;
import build.archipelago.buildserver.common.services.build.BuildService;
import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionSetServiceClient;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SqsMessageHandler;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class BuildRequestHandler implements SqsMessageHandler {

    private VersionSetServiceClient vsClient;
    private PackageServiceClient packageServiceClient;
    private Path workspaceLocation;
    private BuildService buildService;
    private MauiWrapper mauiWrapper;
    private AccountService accountService;

    public BuildRequestHandler(VersionSetServiceClient vsClient,
                               PackageServiceClient packageServiceClient,
                               Path workspaceLocation,
                               MauiWrapper mauiWrapper,
                               BuildService buildService, AccountService accountService) {
        this.vsClient = vsClient;
        this.packageServiceClient = packageServiceClient;
        this.workspaceLocation = workspaceLocation;
        this.buildService = buildService;
        this.mauiWrapper = mauiWrapper;
        this.accountService = accountService;
    }

    @Override
    public void handle(Message message) throws PermanentMessageProcessingException, TemporaryMessageProcessingException {
            log.info("Got request to handle build: " + message.getBody());
            BuildQueueMessage buildMsg;
            try {
                buildMsg = BuildQueueMessage.parse(message.getBody());
            } catch (RuntimeException exp) {
                log.warn(String.format("Failed to parse the build message, may have been a status message: %s", message.getBody()), exp);
                throw new PermanentMessageProcessingException();
            }

            VersionSetBuilder builder = new VersionSetBuilder(
                    vsClient, packageServiceClient, workspaceLocation, buildService, mauiWrapper, accountService,
                    buildMsg.getBuildId());
            builder.build();

    }
}
