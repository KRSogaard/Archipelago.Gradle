package build.archipelago.maui.core.actions;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.harbor.client.HarborClient;
import build.archipelago.maui.common.contexts.WorkspaceContext;
import build.archipelago.maui.common.contexts.WorkspaceContextFactory;
import build.archipelago.maui.common.serializer.WorkspaceSerializer;
import build.archipelago.maui.core.output.OutputWrapper;
import build.archipelago.maui.core.providers.SystemPathProvider;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class WorkspaceCreateAction extends BaseAction {
    private HarborClient harborClient;

    public WorkspaceCreateAction(WorkspaceContextFactory workspaceContextFactory,
                                  SystemPathProvider systemPathProvider,
                                  OutputWrapper out,
                                  HarborClient harborClient) {
        super(workspaceContextFactory, systemPathProvider, out);
        this.harborClient = harborClient;
    }

    public boolean createWorkspace(String name, String versionSet) {
        Path dir = systemPathProvider.getCurrentDir();
        out.write("Creating workspace %s", name);
        Path wsRoot = dir.resolve(name);
        WorkspaceContext ws = workspaceContextFactory.create(wsRoot);

        if (Files.exists(wsRoot)) {
            out.error("The workspace \"%s\" already exists in this folder", name);
            return false;
        }
        try {
            Files.createDirectory(wsRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!Strings.isNullOrEmpty(versionSet)) {
            try {
                // We need to ensure we have the right capitalization
                VersionSet vs = harborClient.getVersionSet(versionSet);
                ws.setVersionSet(vs.getName());
            } catch (VersionSetDoseNotExistsException e) {
                log.error("Was unable to created the workspace as the requested version-set \"{}\" did not exist",
                        versionSet);
                out.error("Was unable to created the workspace as the requested version-set " +
                        "\"%s\" did not exist", versionSet);
                return false;
            }
        }

        try {
            WorkspaceSerializer.save(ws, wsRoot);
        } catch (IOException e) {
            log.error("Failed to create the workspace file in \"" + wsRoot + "\"", e);
            out.error("Was unable to create the workspace file");
            return false;
        }
        return true;
    }
}
