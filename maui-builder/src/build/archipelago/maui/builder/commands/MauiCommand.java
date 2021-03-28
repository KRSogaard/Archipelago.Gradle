package build.archipelago.maui.builder.commands;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "Maui", mixinStandardHelpOptions = true, subcommands = {
        PathCommand.class,
        VersionCommand.class})
public class MauiCommand  implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        return 1;
    }
}
