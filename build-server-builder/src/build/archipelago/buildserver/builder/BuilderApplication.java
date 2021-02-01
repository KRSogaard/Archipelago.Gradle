package build.archipelago.buildserver.builder;

import build.archipelago.buildserver.builder.handlers.BuildRequestHandler;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SQSConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class BuilderApplication implements CommandLineRunner {

    private SQSConsumer sqsConsumer;
    private BuildRequestHandler buildRequestHandler;

    public BuilderApplication(SQSConsumer sqsConsumer,
                              BuildRequestHandler buildRequestHandler) {
        this.sqsConsumer = sqsConsumer;
        this.buildRequestHandler = buildRequestHandler;
    }

    public static void main(String[] args) {
        SpringApplication.run(BuilderApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            Message msg = new Message();
            msg.setBody("{\"buildId\":\"2bc3b949-ad92-4947-a2ce-0655fe4335fe\",\"accountId\":\"wewelo\"}");
            buildRequestHandler.handle(msg);
//            sqsConsumer.start();
//            sqsConsumer.waitForExecutors();
//            log.warn("Executors was shutdown");
        } catch (Exception exp) {
            log.error("Error while executing", exp);
        }
        System.exit(1);
    }
}
