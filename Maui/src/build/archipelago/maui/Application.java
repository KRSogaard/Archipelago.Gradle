package build.archipelago.maui;

import build.archipelago.maui.commands.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import picocli.CommandLine;

@SpringBootApplication
@Slf4j
public class Application {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(new SpringApplicationBuilder(Application.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args)));
    }

//    @Override
//    public void run(String... args) throws Exception {
//
//        int exitCode = new CommandLine(new MauiCommand()).execute(args);
//        System.exit(exitCode);
//    }


}
