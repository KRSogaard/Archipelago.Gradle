package build.archipelago.maui.commands.packages;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.*;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.Output.OutputWrapper;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.providers.SystemPathProvider;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.common.models.BuildConfig;
import build.archipelago.maui.common.serializer.BuildConfigSerializer;
import build.archipelago.packageservice.client.models.CreatePackageRequest;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.*;
import java.util.List;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new package")
public class PackageCreateCommand extends BaseCommand {

    private HarborClient harborClient;

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;
    @CommandLine.Option(names = { "-d", "--desc"})
    private String description;

    public PackageCreateCommand(WorkspaceContextFactory workspaceContextFactory,
                                SystemPathProvider systemPathProvider,
                                OutputWrapper out,
                                HarborClient harborClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace()) {
            out.error("Was unable to locate the workspace");
            return 1;
        }

        if (Strings.isNullOrEmpty(name)) {
            out.error("A package name is required");
            return 1;
        }

        if (!ArchipelagoPackage.validateName(name)) {
            out.error("The package name was not valid.");
            return 1;
        }

        Path pkgDir = wsDir.resolve(name);

        if (workspaceContext.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(name)) ||
            Files.exists(pkgDir)) {
            out.error("A package by the name \"%s\" is already in the workspace", name);
            return 1;
        }

        if (packageNameExists(name)) {
            out.error("A package by the name \"%s\" is already exists, you can check it out.", name);
            return 1;
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
        return 0;
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
