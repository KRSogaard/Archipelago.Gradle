package build.archipelago.buildserver.builder.handlers;

import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SqsMessageHandler;
import com.wewelo.sqsconsumer.exceptions.*;

public class BuildRequestHandler implements SqsMessageHandler {
    @Override
    public void handle(Message message) throws PermanentMessageProcessingException, TemporaryMessageProcessingException {

    }
}
