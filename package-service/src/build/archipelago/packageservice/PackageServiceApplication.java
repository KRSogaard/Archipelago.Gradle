package build.archipelago.packageservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class PackageServiceApplication {

    public static void main(String[] args) {
        log.info("Package Service is starting");
        SpringApplication.run(PackageServiceApplication.class, args);
    }

}