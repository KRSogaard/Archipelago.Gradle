package build.archipelago.buildserver.builder;

import com.wewelo.sqsconsumer.SQSConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private SQSConsumer sqsConsumer;

    public Application(SQSConsumer sqsConsumer) {
        this.sqsConsumer = sqsConsumer;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            sqsConsumer.start();
            while(true) {
                Thread.sleep(250);
            }
            //log.warn("Executors was shutdown");
        } catch (Exception exp) {
            log.error("Error while executing", exp);
        }
    }
}
