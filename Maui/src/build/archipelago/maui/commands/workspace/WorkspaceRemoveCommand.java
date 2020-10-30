package build.archipelago.maui.commands.workspace;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@CommandLine.Command(name = "remove", aliases = {"rm"}, mixinStandardHelpOptions = true, description = "Checkout packages or version-set")
public class WorkspaceRemoveCommand extends BaseCommand {

    @CommandLine.Option(names = { "-p", "--package"})
    private List<String> packages;

    public WorkspaceRemoveCommand(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }

        if (packages == null || packages.size() == 0) {
            out.error("No packages was provided");
        }

        for (final String pkg : packages) {
            if (!ArchipelagoPackage.validateName(pkg)) {
                out.error("The package name \"%s\" is not valid", pkg);
                continue;
            }
            try {
                Optional<String> pkgName = workspaceContext.getLocalPackages().stream()
                        .filter(lp -> lp.equalsIgnoreCase(pkg)).findFirst();
                if (pkgName.isEmpty()) {
                    out.error("The package name \"%s\" is not checked out", pkg);
                    continue;
                }

                // Ensure we have the capitalization of the package name
                String cleanPKGName = pkgName.get();
                Path pkgDir = wsDir.resolve(cleanPKGName);
                if (Files.exists(pkgDir)) {
                    try (Stream<Path> walk = Files.walk(pkgDir)) {
                        walk.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    }
                }
                workspaceContext.removeLocalPackage(cleanPKGName);
                workspaceContext.save();

                out.write("Successfully added %s to the workspace", cleanPKGName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return 0;
    }
}
