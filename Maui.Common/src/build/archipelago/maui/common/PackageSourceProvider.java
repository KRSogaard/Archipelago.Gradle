package build.archipelago.maui.common;

import java.nio.file.Path;

public interface PackageSourceProvider {
    boolean checkOutSource(Path workspaceRoot, String packageName);
    boolean checkOutSource(Path workspaceRoot, String packageName, String commit);
}
