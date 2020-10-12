package build.archipelago.buildserver.builder.handlers;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SqsMessageProcessingFailureHandler;
import com.wewelo.sqsconsumer.exceptions.*;

public class BuildRequestFailureHandler implements SqsMessageProcessingFailureHandler {
    @Override
    public void handlePermanentFailure(AmazonSQS amazonSQS, Message message, PermanentMessageProcessingException e) {

    }

    @Override
    public void handleTemporaryFailure(AmazonSQS amazonSQS, Message message, TemporaryMessageProcessingException e) {

    }
}
