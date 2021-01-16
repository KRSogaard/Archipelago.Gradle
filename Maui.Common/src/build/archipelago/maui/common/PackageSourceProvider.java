package build.archipelago.maui.common;

import build.archipelago.packageservice.models.PackageDetails;

import java.nio.file.Path;

public interface PackageSourceProvider {
    boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails);
    boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails, String commit);
}
