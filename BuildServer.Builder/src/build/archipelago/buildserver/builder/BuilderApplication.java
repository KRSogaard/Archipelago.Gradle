package build.archipelago.buildserver.builder;

import com.wewelo.sqsconsumer.SQSConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;

@Slf4j
public class BuilderApplication implements CommandLineRunner {

    private SQSConsumer sqsConsumer;

    public BuilderApplication(SQSConsumer sqsConsumer) {
        this.sqsConsumer = sqsConsumer;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            sqsConsumer.start();
            sqsConsumer.waitForExecutors();
            log.warn("Executors was shutdown");
        } catch (Exception exp) {
            log.error("Error while executing", exp);
        }
    }
}
