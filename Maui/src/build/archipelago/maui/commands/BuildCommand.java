package build.archipelago.maui.commands;

import build.archipelago.maui.core.actions.BuildAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.graph.DependencyTransversalType;
import build.archipelago.maui.path.MauiPath;
import build.archipelago.maui.path.recipies.BinRecipe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import picocli.CommandLine;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

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
