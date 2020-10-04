package build.archipelago.maui.core.workspace;

import java.nio.file.Path;

public interface PackageSourceProvider {
    boolean checkOutSource(String packageName, Path workspaceRoot);
}
