package build.archipelago.maui.core.workspace.serializer;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.*;
import build.archipelago.maui.core.workspace.models.BuildConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class BuildConfigSerializer {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private String buildSystem;
    private List<String> libraries;
    private List<String> buildTools;
    private List<String> test;
    private List<String> runtime;
    private List<String> removeDependencies;
    private List<String> resolveConflicts;

    private static BuildConfigSerializer convert(BuildConfig bc) {
        List<String> localPackages = new ArrayList<String>();
        BuildConfigSerializer serializer = new BuildConfigSerializer();
        serializer.setBuildSystem(bc.getBuildSystem());
        serializer.setLibraries(bc.getLibraries().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        serializer.setBuildTools(bc.getBuildTools().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        serializer.setTest(bc.getTest().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        serializer.setRuntime(bc.getRuntime().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        serializer.setRemoveDependencies(bc.getRemoveDependencies().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        serializer.setResolveConflicts(bc.getResolveConflicts().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));

        return serializer;
    }

    private static BuildConfig convert(BuildConfigSerializer bcs) {
        return BuildConfig.builder()
                .buildSystem(bcs.getBuildSystem())
                .libraries(bcs.getLibraries().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .buildTools(bcs.getBuildTools().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .test(bcs.getTest().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .runtime(bcs.getRuntime().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .removeDependencies(bcs.getRemoveDependencies().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .resolveConflicts(bcs.getResolveConflicts().stream().map(ArchipelagoPackage::parse)
                        .collect(Collectors.toList()))
                .build();
    }

    public static void save(BuildConfig buildConfig, Path packageRoot) throws IOException {
        if (Files.notExists(packageRoot)) {
            throw new IOException(String.format("Package root \"%s\" was not found", packageRoot));
        }

        Path buildFilePath = packageRoot.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (Files.exists(buildFilePath)) {
            log.warn("Workspace file \"{}\" already exists, we will override it", buildFilePath.toString());
        }

        BuildConfigSerializer bcs = BuildConfigSerializer.convert(buildConfig);
        mapper.writeValue(buildFilePath.toFile(), bcs);
    }

    public static BuildConfig load(Path packageRoot) throws IOException {
        Path buildFilePath = packageRoot.resolve(WorkspaceConstants.BUILD_FILE_NAME);
        if (Files.notExists(buildFilePath)) {
            throw new IOException(String.format("Could not find the package file \"%s\" in \"%s\"",
                    WorkspaceConstants.BUILD_FILE_NAME, packageRoot));
        }

        BuildConfigSerializer bcs = mapper.readValue(buildFilePath.toFile(), BuildConfigSerializer.class);
        return BuildConfigSerializer.convert(bcs);
    }
}
