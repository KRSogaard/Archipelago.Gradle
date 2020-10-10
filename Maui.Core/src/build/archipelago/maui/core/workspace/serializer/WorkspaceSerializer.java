package build.archipelago.maui.core.workspace.serializer;

import build.archipelago.maui.core.workspace.WorkspaceConstants;
import build.archipelago.maui.core.workspace.models.Workspace;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Data
public class WorkspaceSerializer {
    private static final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    private String versionSet;
    private List<String> localPackages;

    public WorkspaceSerializer() {}

    private static WorkspaceSerializer convert(Workspace ws) {
        List<String> localPackages = new ArrayList<String>();
        for (String name : ws.getLocalPackages()) {
            localPackages.add(name);
        }

        WorkspaceSerializer wss = new WorkspaceSerializer();
        wss.setVersionSet(ws.getVersionSet());
        wss.setLocalPackages(localPackages);
        return wss;
    }

    private static Workspace convert(WorkspaceSerializer wss) {
        return Workspace.builder()
                .versionSet(wss.getVersionSet())
                .localPackages(wss.getLocalPackages())
                .build();
    }

    public static void save(Workspace workspace, Path workspaceRoot) throws IOException {
        if (Files.notExists(workspaceRoot)) {
            throw new IOException(String.format("Workspace root \"%s\" was not found", workspaceRoot));
        }

        Path workspaceFilePath = workspaceRoot.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME);
        if (Files.exists(workspaceFilePath)) {
            log.warn("Workspace file \"{}\" already exists, we will override it", workspaceFilePath.toString());
        }

        WorkspaceSerializer wss = WorkspaceSerializer.convert(workspace);
        mapper.writeValue(workspaceFilePath.toFile(), wss);
    }

    public static Workspace load(Path workspaceRoot) throws IOException {
        Path workspaceFilePath = workspaceRoot.resolve(WorkspaceConstants.WORKSPACE_FILE_NAME);
        if (Files.notExists(workspaceFilePath)) {
            throw new IOException(String.format("Could not find the workspace file \"%s\" in \"%s\"",
                    WorkspaceConstants.WORKSPACE_FILE_NAME, workspaceRoot));
        }

        WorkspaceSerializer wss = mapper.readValue(workspaceFilePath.toFile(), WorkspaceSerializer.class);
        return WorkspaceSerializer.convert(wss);
    }

}
