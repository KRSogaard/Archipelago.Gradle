package build.archipelago.maui.core.workspace.models;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.serializer.BuildConfigSerializer;
import lombok.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Data
@Builder
public class BuildConfig {

    private String buildSystem;
    private List<ArchipelagoPackage> libraries;
    private List<ArchipelagoPackage> buildTools;
    private List<ArchipelagoPackage> test;
    private List<ArchipelagoPackage> runtime;
    private List<ArchipelagoPackage> removeDependencies;
    private List<ArchipelagoPackage> resolveConflicts;

    public static BuildConfig from(Path path) throws IOException {
        return BuildConfigSerializer.load(path);
    }
}
