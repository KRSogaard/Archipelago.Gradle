package build.archipelago.maui.core.workspace;

import build.archipelago.common.ArchipelagoPackage;
import java.util.List;

public class Build {
    private String buildSystem;
    private String version;

    private List<ArchipelagoPackage> dependencies;
    private List<ArchipelagoPackage> buildDependencies;
    private List<ArchipelagoPackage> testDependencies;
    private List<ArchipelagoPackage> runtimeDependencies;
}
