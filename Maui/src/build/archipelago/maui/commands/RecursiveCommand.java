package build.archipelago.maui.commands;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.actions.BaseAction;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.graph.*;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "recursive", aliases = {"rec"}, mixinStandardHelpOptions = true, description = "Recursive commands")
public class RecursiveCommand extends BaseAction implements Callable<Integer> {

    private DependencyGraphGenerator dependencyGraphGenerator;

    @CommandLine.Parameters(index = "0..*")
    private String[] args;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec commandSpec;

    public RecursiveCommand(WorkspaceContextFactory workspaceContextFactory,
                            SystemPathProvider systemPathProvider,
                            OutputWrapper out,
                            DependencyGraphGenerator dependencyGraphGenerator) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.dependencyGraphGenerator = dependencyGraphGenerator;
    }

    @Override
    public Integer call() throws Exception {
        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }
        if (!setupPackage()) {
            out.error("Was unable to locate the package");
            return 1;
        }

        if (args.length == 0) {
            out.error("No parameters where given for the recursive command to run");
            return 1;
        }
        if ("recursive".equalsIgnoreCase(args[0]) || "rec".equalsIgnoreCase(args[0])) {
            out.error("You can not use the recursive command on a recursive command");
            return 1;
        }

        ArchipelagoDependencyGraph graph = dependencyGraphGenerator.generateGraph(workspaceContext, commandPKG, DependencyTransversalType.BUILD_TOOLS);

        CommandLine commandLine = getTopCommandLine();

        for (ArchipelagoPackage pkg : OrderedGraphTraversal.bottomUpTransversal(graph, commandPKG)) {
            if (workspaceContext.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(pkg.getName()))) {
                Path pkgRoot = workspaceContext.getPackageRoot(pkg);
                systemPathProvider.overrideCurrentDir(pkgRoot);
                out.write("Running \"%s\" on package %s", String.join(" ", args), pkg.getName());
                int outcome = commandLine.execute(args);
                if (outcome != 0) {
                    out.error("Recursive command \"%s\" failed on package %s", String.join(" ", args), pkg.getName());
                    return outcome;
                }
                systemPathProvider.removeCurrentDirOverride();
            }
        }

        return 0;
    }

    private CommandLine getTopCommandLine() {
        CommandLine commandLine = commandSpec.commandLine();
        while (commandLine != null) {
            if (commandLine.getParent() == null) {
                return commandLine;
            }
            commandLine = commandLine.getParent();
        }
        throw new RuntimeException("Was unable to find top CommandLine");
    }
}
