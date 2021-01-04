package build.archipelago.maui.core.actions;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageExistsException;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.common.models.BuildConfig;
import build.archipelago.maui.common.serializer.BuildConfigSerializer;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import com.google.common.base.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PackageCreateAction extends BaseAction {
    private HarborClient harborClient;

    public PackageCreateAction(WorkspaceContextFactory workspaceContextFactory,
                               SystemPathProvider systemPathProvider,
                               OutputWrapper out,
                               HarborClient harborClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
    }

    public boolean createPackageName(String name, String description) throws Exception {
        if (!setupWorkspaceContext()) {
            out.error("Was unable to locate the workspace");
            return false;
        }

        if (Strings.isNullOrEmpty(name)) {
            out.error("A package name is required");
            return false;
        }

        if (!ArchipelagoPackage.validateName(name)) {
            out.error("The package name was not valid.");
            return false;
        }

        Path pkgDir = wsDir.resolve(name);

        if (workspaceContext.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(name)) ||
                Files.exists(pkgDir)) {
            out.error("A package by the name \"%s\" is already in the workspace", name);
            return false;
        }

        if (packageNameExists(name)) {
            out.error("A package by the name \"%s\" is already exists, you can check it out.", name);
            return false;
        }

        try {
            harborClient.createPackage(CreatePackageRequest.builder()
                    .name(name)
                    .description(description)
                    .build());
        } catch (PackageExistsException exp) {
            out.error("Failed to create the package");
        }

        Files.createDirectory(pkgDir);
        BuildConfig config = BuildConfig.builder()
                .buildSystem("Copy")
                .version("1.0")
                .buildTools(List.of(new ArchipelagoPackage("CopyBuildSystem", "1.0")))
                .build();
        BuildConfigSerializer.save(config, pkgDir);
        Files.writeString(pkgDir.resolve("emptyPackage.txt"), "This is an empty package");

        workspaceContext.getLocalPackages().add(name);
        workspaceContext.save();
        return true;
    }

    private boolean packageNameExists(String name) {
        try {
            harborClient.getPackage(name);
            return true;
        } catch (PackageNotFoundException e) {
            return false;
        }
    }
}
