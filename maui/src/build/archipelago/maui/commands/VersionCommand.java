package build.archipelago.maui.commands;

import build.archipelago.maui.core.output.OutputWrapper;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "version", mixinStandardHelpOptions = true, description = "Get maui version")
public class VersionCommand implements Callable<Integer> {

    private final OutputWrapper out;

    public VersionCommand(OutputWrapper out) {
        this.out = out;
    }

    @Override
    public Integer call() throws Exception {
        out.write("Maui version 1.0");
        return 0;
    }
}
