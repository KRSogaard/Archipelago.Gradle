package build.archipelago.maui.commands;

import build.archipelago.maui.core.actions.CleanAction;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "clean", mixinStandardHelpOptions = true, description = "Build a package")
public class CleanCommand implements Callable<Integer> {

    private final CleanAction cleanAction;

    public CleanCommand(CleanAction cleanAction) {
        this.cleanAction = cleanAction;
    }

    @Override
    public Integer call() throws Exception {
        if (cleanAction.clean()) {
            return 0;
        }
        return 1;
    }
}
