package build.archipelago.maui.common.models;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.PackageNotLocalException;
import build.archipelago.maui.common.serializer.BuildConfigSerializer;
import lombok.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Data
@Builder
public class BuildConfig {

    private String buildSystem;
    private String version;
    private List<ArchipelagoPackage> libraries;
    private List<ArchipelagoPackage> buildTools;
    private List<ArchipelagoPackage> test;
    private List<ArchipelagoPackage> runtime;
    private List<ArchipelagoPackage> removeDependencies;
    private List<ArchipelagoPackage> resolveConflicts;

    public static BuildConfig from(Path path) throws PackageNotLocalException {
        return BuildConfigSerializer.load(path);
    }

    public List<ArchipelagoPackage> getAllDependencies() {
        // We only want unique dependencies
        Map<ArchipelagoPackage, Boolean> map = new HashMap<>();
        if (libraries != null) {
            libraries.forEach(p -> map.put(p, true));
        }
        if (buildTools != null) {
            buildTools.forEach(p -> map.put(p, true));
        }
        if (test != null) {
            test.forEach(p -> map.put(p, true));
        }
        if (runtime != null) {
            runtime.forEach(p -> map.put(p, true));
        }

        return new ArrayList<>(map.keySet());
    }
}
