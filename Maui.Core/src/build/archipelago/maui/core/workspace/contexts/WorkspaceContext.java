package build.archipelago.maui.core.workspace.contexts;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.*;
import build.archipelago.maui.core.exceptions.*;
import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.models.Workspace;
import build.archipelago.maui.core.workspace.serializer.*;
import build.archipelago.versionsetservice.client.VersionServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;

@Slf4j
public class WorkspaceContext extends Workspace {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    private Path root;
    private VersionServiceClient vsClient;

    private VersionSetRevision versionSetRevision;

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

    // TODO: Change this to be aware of the cached packages
    public Path getPackageRoot(ArchipelagoPackage pkg) throws PackageNotLocalException {
        Path packagePath = root.resolve(pkg.getName());
        if (!Files.exists(packagePath)) {
            log.warn("The requested package \"{}\" is not in the workspaces root \"{}\"",
                    pkg.getNameVersion(), root);
            throw new PackageNotLocalException(pkg);
        }
        return packagePath;
    }

    public void saveRevisionCache(VersionSetRevision vsRevision) throws IOException {
        Path revisionPath = getRevisionCachePath();
        if (Files.exists(revisionPath)) {
            Files.delete(revisionPath);
        }
        mapper.writeValue(revisionPath.toFile(), vsRevision);
    }

    public VersionSetRevision getVersionSetRevision() throws IOException, VersionSetNotSyncedException {
        if (this.versionSetRevision == null) {
            Path revisionPath = getRevisionCachePath();
            if (!Files.exists(revisionPath)) {
                throw new VersionSetNotSyncedException();
            }
            this.versionSetRevision = mapper.readValue(revisionPath.toFile(), VersionSetRevision.class);
        }
        return this.versionSetRevision;
    }

    private Path getRevisionCachePath() throws IOException {
        Path workspaceTempDir = root.resolve(WorkspaceConstants.TEMP_FOLDER);
        if (!Files.exists(workspaceTempDir)) {
            Files.createDirectory(workspaceTempDir);
        }
        return workspaceTempDir.resolve(WorkspaceConstants.VERSION_SET_REVISION_CACHE);
    }
}
