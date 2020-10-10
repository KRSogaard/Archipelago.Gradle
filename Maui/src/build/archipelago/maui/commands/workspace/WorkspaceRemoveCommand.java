package build.archipelago.maui.commands.workspace;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
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
                                  SystemPathProvider systemPathProvider) {
        super(workspaceContextFactory, systemPathProvider);
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }

        if (packages == null || packages.size() == 0) {
            System.err.println("No packages was provided");
        }

        for (final String pkg : packages) {
            if (!ArchipelagoPackage.validateName(pkg)) {
                System.err.println(String.format("The package name \"%s\" is not valid", pkg));
                continue;
            }
            try {
                Optional<String> pkgName = workspaceContext.getLocalPackages().stream()
                        .filter(lp -> lp.equalsIgnoreCase(pkg)).findFirst();
                if (pkgName.isEmpty()) {
                    System.err.println(String.format("The package name \"%s\" is not checked out", pkg));
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

                System.out.println(String.format("Successfully added %s to the workspace", cleanPKGName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return 0;
    }
}
