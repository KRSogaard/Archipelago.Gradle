package build.archipelago.maui.common;

import java.nio.file.Path;

public interface PackageSourceProvider {
    boolean checkOutSource(String packageName, Path workspaceRoot);
}
