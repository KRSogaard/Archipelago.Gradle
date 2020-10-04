package build.archipelago.maui.commands.packages;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotFoundException;
import build.archipelago.maui.commands.BaseCommand;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.cache.PackageCacher;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import build.archipelago.maui.core.workspace.serializer.BuildConfigSerializer;
import build.archipelago.packageservice.client.PackageServiceClient;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(name = "create", mixinStandardHelpOptions = true, description = "create a new package")
public class PackageCreateCommand extends BaseCommand {

    private VersionServiceClient vsClient;
    private PackageCacher packageCacher;
    private PackageServiceClient packageClient;

    @CommandLine.Option(names = { "-n", "--name"}, required = true)
    private String name;

    public PackageCreateCommand(VersionServiceClient vsClient,
                                PackageCacher packageCacher,
                                PackageServiceClient packageClient) {
        this.vsClient = vsClient;
        this.packageCacher = packageCacher;
        this.packageClient = packageClient;
    }

    @Override
    public Integer call() throws Exception {
        if (!requireWorkspace(vsClient, packageCacher)) {
            System.err.println("Was unable to locate the workspace");
            return 1;
        }

        if (Strings.isNullOrEmpty(name)) {
            System.err.println("A package name is required");
            return 1;
        }

        if (!ArchipelagoPackage.validateName(name)) {
            System.err.println("The package name was not valid.");
            return 1;
        }

        Path pkgDir = wsDir.resolve(name);

        if (ws.getLocalPackages().stream().anyMatch(lp -> lp.equalsIgnoreCase(name)) ||
            Files.exists(pkgDir)) {
            System.err.println(String.format("A package by the name \"%s\" is already in the workspace", name));
            return 1;
        }

        if (packageNameExists(name)) {
            System.err.println(String.format("A package by the name \"%s\" is already exists, you can check it out.", name));
            return 1;
        }

        Files.createDirectory(pkgDir);
        BuildConfig config = BuildConfig.builder()
                                .buildSystem("Copy")
                                .version("1.0")
                                .buildTools(List.of(new ArchipelagoPackage("CopyBuildSystem", "1.0")))
                                .build();
        BuildConfigSerializer.save(config, pkgDir);
        Files.writeString(pkgDir.resolve("emptyPackage.txt"), "This is an empty package");

        ws.getLocalPackages().add(name);
        ws.save();
        return 0;
    }

    private boolean packageNameExists(String name) {
        try {
            packageClient.getPackage(name);
            return true;
        } catch (PackageNotFoundException e) {
            return false;
        }
    }

}
