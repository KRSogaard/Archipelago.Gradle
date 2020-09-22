package build.archipelago.maui.core.context;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.common.workspace.Workspace;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import buils.archipelago.maui.serializer.*;

import java.io.IOException;
import java.nio.file.*;

public class WorkspaceContext extends Workspace {

    private Path root;
    private VersionServiceClient vsClient;

    public WorkspaceContext(Path root, VersionServiceClient vsClient) throws IOException {
        super();
        this.root = root;
        this.vsClient = vsClient;

        Path workspaceFile = getWorkspaceFile(root);
        if (Files.exists(workspaceFile)) {
            load(workspaceFile);
        }
    }

    private void load(Path path) throws IOException {
        Workspace ws = WorkspaceSerializer.load(path);
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
        return Paths.get(path.toString(), WorkspaceConstants.WORKSPACE_FILE_NAME);
    }
}
