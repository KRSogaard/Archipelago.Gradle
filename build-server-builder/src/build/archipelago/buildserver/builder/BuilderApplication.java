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
//            Message msg = new Message();
//            msg.setBody("{\"buildId\":\"c2695def\",\"accountId\":\"wewelo\"}");
//            buildRequestHandler.handle(msg);
            log.info("Starting SQS Consumer");
            sqsConsumer.start();
            while(true) {
                Thread.sleep(1000);
            }
            //sqsConsumer.waitForExecutors();
//            log.warn("Executors was shutdown");
        } catch (Exception exp) {
            log.error("Error while executing", exp);
        }
        System.exit(1);
    }
}
