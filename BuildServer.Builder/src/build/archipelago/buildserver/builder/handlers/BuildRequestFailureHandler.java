package build.archipelago.buildserver.builder.handlers;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SqsMessageProcessingFailureHandler;
import com.wewelo.sqsconsumer.exceptions.PermanentMessageProcessingException;
import com.wewelo.sqsconsumer.exceptions.TemporaryMessageProcessingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildRequestFailureHandler implements SqsMessageProcessingFailureHandler {

    private String queueUrl;

    public BuildRequestFailureHandler(String queueUrl) {
        this.queueUrl = queueUrl;
    }

    @Override
    public void handlePermanentFailure(AmazonSQS amazonSQS, Message message, PermanentMessageProcessingException e) {
        amazonSQS.deleteMessage(queueUrl, message.getReceiptHandle());
    }

    @Override
    public void handleTemporaryFailure(AmazonSQS amazonSQS, Message message, TemporaryMessageProcessingException e) {

    }

    @Override
    public void handleRuntimeFailure(AmazonSQS amazonSQS, Message message, RuntimeException e) {
        log.error("Encountered an runtime exception while executing a request", e);
    }
}
