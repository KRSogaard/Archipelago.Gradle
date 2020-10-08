package build.archipelago.maui.commands.workspace;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.core.workspace.PackageSourceProvider;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.contexts.WorkspaceContextFactory;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.packageservice.client.models.GetPackageResponse;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.inject.internal.util.SourceProvider;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Files;
import java.util.List;

@Slf4j
@CommandLine.Command(name = "use", mixinStandardHelpOptions = true, description = "Checkout packages or version-set")
public class WorkspaceUseCommand extends BaseCommand {

    @CommandLine.Option(names = { "-p", "--package"})
    private List<String> packages;

    @CommandLine.Option(names = { "-vs", "--versionset"})
    private String versionSet;

    private PackageServiceClient packageServiceClient;
    private PackageSourceProvider packageSourceProvider;
    private VersionServiceClient vsClient;

    public WorkspaceUseCommand(
                VersionServiceClient vsClient,
                WorkspaceContextFactory workspaceContextFactory,
                SystemPathProvider systemPathProvider,
                PackageServiceClient packageServiceClient,
                PackageSourceProvider packageSourceProvider) {
            super(workspaceContextFactory, systemPathProvider);
        this.vsClient = vsClient;
        this.packageServiceClient = packageServiceClient;
        this.packageSourceProvider = packageSourceProvider;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }

        if (versionSet != null) {
            if (versionSet.equalsIgnoreCase(ws.getVersionSet())) {
                System.err.println(String.format("The version-set for the workspace is already \"%s\"", ws.getVersionSet()));
            } else {
                try {
                    VersionSet vs = vsClient.getVersionSet(versionSet);
                    System.out.println(String.format("Setting the version-set for the workspace to \"%s\"", vs.getName()));
                    // Ensure we have the right capitalization of the version-set
                    ws.setVersionSet(vs.getName());
                    ws.save();
                    // We clear the cache as it would have been the old version-set
                    ws.clearVersionSetRevisionCache();
                } catch (VersionSetDoseNotExistsException e) {
                    System.err.println(String.format("The version-set \"%s\" dose not exists", versionSet));
                }
            }
        }

        if (packages != null) {
            for (final String pkg : packages) {
                if (!ArchipelagoPackage.validateName(pkg)) {
                    System.err.println(String.format("The package name \"%s\" is not valid", pkg));
                    continue;
                }
                try {
                    if (ws.getLocalArchipelagoPackages().stream().anyMatch(lp -> lp.getName().equalsIgnoreCase(pkg))) {
                        System.err.println(String.format("The package name \"%s\" already checked out", pkg));
                        continue;
                    }
                    GetPackageResponse pkgResponse = packageServiceClient.getPackage(pkg);
                    // Ensure we have the capitalization of the package name
                    String cleanPKGName = pkgResponse.getName();
                    if (Files.exists(wsDir.resolve(cleanPKGName))) {
                        System.err.println(String.format("Directory %s already exists, please remove or rename it " +
                                "before checking out the package %s", cleanPKGName, cleanPKGName));
                        continue;
                    }

                    if (!packageSourceProvider.checkOutSource(cleanPKGName, wsDir)) {
                        System.err.println(String.format("Failed to checkout the source for the package \"%s\"", pkg));
                        continue;
                    }

                    ws.addLocalPackage(cleanPKGName);
                    ws.save();

                    System.out.println(String.format("Successfully added %s to the workspace", cleanPKGName));
                } catch (PackageNotFoundException exp) {
                    System.err.println(String.format("The package name \"%s\" dose not exists", pkg));
                }
            }
        }

        return 0;
    }
}
