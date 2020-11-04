package build.archipelago.maui.common.serializer;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.WorkspaceConstants;
import build.archipelago.maui.common.models.BuildConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Data
public class BuildConfigSerializer {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private String buildSystem;
    private String version;
    private List<String> libraries;
    private List<String> buildTools;
    private List<String> test;
    private List<String> runtime;
    private List<String> removeDependencies;
    private List<String> resolveConflicts;

    private static BuildConfigSerializer convert(BuildConfig bc) {
        BuildConfigSerializer serializer = new BuildConfigSerializer();
        serializer.setBuildSystem(bc.getBuildSystem());
        serializer.setVersion(bc.getVersion());
        if (bc.getLibraries() != null) {
            serializer.setLibraries(bc.getLibraries().stream().map(ArchipelagoPackage::getNameVersion)
                    .collect(Collectors.toList()));
        }
        if (bc.getBuildTools() != null) {
        serializer.setBuildTools(bc.getBuildTools().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        }
        if (bc.getTest() != null) {
        serializer.setTest(bc.getTest().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        }
        if (bc.getRuntime() != null) {
        serializer.setRuntime(bc.getRuntime().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        }
        if (bc.getRemoveDependencies() != null) {
        serializer.setRemoveDependencies(bc.getRemoveDependencies().stream().map(ArchipelagoPackage::getNameVersion)
                .collect(Collectors.toList()));
        }
        if (bc.getResolveConflicts() != null) {
            serializer.setResolveConflicts(bc.getResolveConflicts().stream().map(ArchipelagoPackage::getNameVersion)
                    .collect(Collectors.toList()));
        }

        return serializer;
    }

    private static BuildConfig convert(BuildConfigSerializer bcs) {
        BuildConfig.BuildConfigBuilder builder = BuildConfig.builder();
        if (!Strings.isNullOrEmpty(bcs.getBuildSystem())) {
            builder.buildSystem(bcs.getBuildSystem());
        }
        if (!Strings.isNullOrEmpty(bcs.getVersion())) {
            builder.version(bcs.getVersion());
        }
        if (bcs.getLibraries() != null) {
            builder.libraries(bcs.getLibraries().stream().map(ArchipelagoPackage::parse)
                    .collect(Collectors.toList()));
        }
        if (bcs.getBuildTools() != null) {
            builder.buildTools(bcs.getBuildTools().stream().map(ArchipelagoPackage::parse)
                    .collect(Collectors.toList()));
        }
        if (bcs.getTest() != null) {
            builder.test(bcs.getTest().stream().map(ArchipelagoPackage::parse)
                    .collect(Collectors.toList()));
        }
        if (bcs.getRemoveDependencies() != null) {
            builder.removeDependencies(bcs.getRemoveDependencies().stream().map(ArchipelagoPackage::parse)
                    .collect(Collectors.toList()));
        }
        if (bcs.getResolveConflicts() != null) {
            builder.resolveConflicts(bcs.getResolveConflicts().stream().map(ArchipelagoPackage::parse)
                    .collect(Collectors.toList()));
        }
        return builder.build();
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
