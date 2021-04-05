package build.archipelago.maui.common;

import build.archipelago.common.git.models.exceptions.RepoNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;

import java.nio.file.Path;

public interface PackageSourceProvider {
    boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails) throws RepoNotFoundException;
    boolean checkOutSource(Path workspaceRoot, PackageDetails packageDetails, String commit) throws RepoNotFoundException;
}
