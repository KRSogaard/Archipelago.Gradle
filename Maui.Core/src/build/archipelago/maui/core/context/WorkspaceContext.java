package build.archipelago.maui.core.context;

import build.archipelago.common.constants.ArchipelagoFiles;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.maui.common.workspace.Workspace;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import buils.archipelago.maui.serializer.WorkspaceSerializer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        if (Files.exists(root)) {
            throw new RuntimeException("Workspace folder \"" + root + "\" already exists");
        }

        if (versionSet != null) {
             VersionSet vs = vsClient.getVersionSet(versionSet);
             versionSet = vs.getName();
        }

        Files.createDirectory(root);
        WorkspaceSerializer.save(this, getWorkspaceFile(root));
    }

    public static Path getWorkspaceFile(Path path) {
        return Paths.get(path.toString(), ArchipelagoFiles.WORKSPACE);
    }
}
