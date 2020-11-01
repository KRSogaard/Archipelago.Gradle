package build.archipelago.maui.commands;

import build.archipelago.maui.core.actions.CleanAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

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
