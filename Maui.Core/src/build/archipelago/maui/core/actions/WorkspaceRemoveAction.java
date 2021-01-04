package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class WorkspaceRemoveAction extends BaseAction {
    public WorkspaceRemoveAction(WorkspaceContextFactory workspaceContextFactory, SystemPathProvider systemPathProvider, OutputWrapper out) {
        super(workspaceContextFactory, systemPathProvider, out);
    }

    public boolean removePackages(List<String> packages) throws Exception {
        if (packages == null || packages.size() == 0) {
            out.error("No packages was provided");
            return false;
        }

        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
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

        return true;
    }
}
