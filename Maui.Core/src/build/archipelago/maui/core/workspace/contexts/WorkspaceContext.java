package build.archipelago.maui.core.workspace.contexts;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.core.exceptions.PackageNotLocalException;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.models.Workspace;
import build.archipelago.maui.core.workspace.serializer.*;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;

@Slf4j
public class WorkspaceContext extends Workspace {

    private Path root;
    private VersionServiceClient vsClient;

    public WorkspaceContext(Path root, VersionServiceClient vsClient) throws IOException {
        super();
        this.root = root;
        this.vsClient = vsClient;
    }

    public void load() throws FileNotFoundException, IOException {
        Path workspaceFile = getWorkspaceFile(root);
        if (!Files.exists(workspaceFile)) {
            throw new FileNotFoundException(workspaceFile.toString());
        }

        Workspace ws = WorkspaceSerializer.load(root);
        this.setVersionSet(ws.getVersionSet());
        this.setLocalPackages(ws.getLocalPackages());
    }

    public void create() throws VersionSetDoseNotExistsException, IOException {
        if (versionSet != null) {
            // Verifying that the version-set exists and that we have the correct capitalisation of the version-set
             VersionSet vs = vsClient.getVersionSet(versionSet);
             versionSet = vs.getName();
        }
        WorkspaceSerializer.save(this, root);
    }

    public static Path getWorkspaceFile(Path path) {
        return path.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME);
    }

    public Path getPackageRoot(ArchipelagoPackage pkg) throws PackageNotLocalException {
        Path packagePath = root.resolve(pkg.getName());
        if (!Files.exists(packagePath)) {
            log.warn("The requested package \"{}\" is not in the workspaces root \"{}\"",
                    pkg.getNameVersion(), root);
            throw new PackageNotLocalException(pkg);
        }
        return packagePath;
    }
}
