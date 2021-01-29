package build.archipelago.maui.commands;

import build.archipelago.maui.core.actions.BuildAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true, description = "Build a package")
public class BuildCommand implements Callable<Integer> {

    private BuildAction buildAction;

    @CommandLine.Parameters(index = "0..*")
    private List<String> args;

    public BuildCommand(BuildAction buildAction) {
        this.buildAction = buildAction;
    }

    @Override
    public Integer call() throws Exception {
        if (buildAction.build(args)) {
            return 0;
        }
        return 1;
    }
}
