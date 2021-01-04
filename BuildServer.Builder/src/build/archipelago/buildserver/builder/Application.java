package build.archipelago.buildserver.builder;

import build.archipelago.buildserver.builder.handlers.BuildRequestHandler;
import com.amazonaws.services.sqs.model.Message;
import com.wewelo.sqsconsumer.SQSConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private SQSConsumer sqsConsumer;
    private BuildRequestHandler buildRequestHandler;

    public Application(SQSConsumer sqsConsumer,
                       BuildRequestHandler buildRequestHandler) {
        this.sqsConsumer = sqsConsumer;
        this.buildRequestHandler = buildRequestHandler;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            Message msg = new Message();
            msg.setBody("{\"buildId\":\"123-456\"}");
            buildRequestHandler.handle(msg);
            //sqsConsumer.start();
//            while(true) {
//                Thread.sleep(250);
//            }
            //log.warn("Executors was shutdown");
        } catch (Exception exp) {
            log.error("Error while executing", exp);
        }
    }
}
