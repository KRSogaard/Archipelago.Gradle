package build.archipelago.maui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private static String regex = "^[aA-zZ\\-_]+$";
//    private static Map<String, Command> commands;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        ServiceLoader<Command> loader = ServiceLoader.load(Command.class);
//        for (Command implClass : loader) {
//            System.out.println(implClass.getClass().getSimpleName()); // prints Dog, Cat
//        }
    }



//    private void addCommand(Command cmd) {
//        cmd.getNames().stream()
//                .map(n -> n.toLowerCase().trim())
//                .forEach(n -> {
//            Pattern p = Pattern.compile("[a-z]");
//            Matcher m = p.matcher(n);
//            if (!m.find()) {
//                throw new RuntimeException("The alias \"" + n + "\" is not valid.");
//            }
//            if (commands.containsKey(n)) {
//                throw new RuntimeException("A command with the keyword " + n + " already exists.");
//            }
//
//            commands.put(n, cmd);
//        }
//        );
//    }
}
