package build.archipelago.buildserver.builder.handlers;

import build.archipelago.buildserver.builder.builder.*;
import build.archipelago.buildserver.common.services.build.models.BuildQueueMessage;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SqsMessageHandler;
import com.wewelo.sqsconsumer.exceptions.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildRequestHandler implements SqsMessageHandler {

    private BuilderFactory builderFactory;

    public BuildRequestHandler(BuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
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

        builderFactory.create(buildMsg.getBuildId()).build();
    }
}
