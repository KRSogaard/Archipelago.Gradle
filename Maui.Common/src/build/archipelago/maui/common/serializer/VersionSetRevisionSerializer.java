package build.archipelago.maui.common.serializer;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.exceptions.VersionSetNotSyncedException;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.maui.common.WorkspaceConstants;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Data
public class VersionSetRevisionSerializer {
    private static final ObjectMapper mapper = new ObjectMapper(new JsonFactory());

    private long created;
    private List<String> packages;

    public VersionSetRevisionSerializer() {
    }

    private static VersionSetRevisionSerializer convert(VersionSetRevision vsr) {
        Preconditions.checkNotNull(vsr);

        List<String> packages = new ArrayList<>();
        for (ArchipelagoBuiltPackage pkg : vsr.getPackages()) {
            packages.add(pkg.getBuiltPackageName());
        }

        VersionSetRevisionSerializer vsrs = new VersionSetRevisionSerializer();
        vsrs.setCreated(vsr.getCreated().toEpochMilli());
        vsrs.setPackages(packages);
        return vsrs;
    }

    private static VersionSetRevision convert(VersionSetRevisionSerializer vsrs) {
        Preconditions.checkNotNull(vsrs);

        return VersionSetRevision.builder()
                .created(Instant.ofEpochMilli(vsrs.getCreated()))
                .packages(vsrs.getPackages().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList()))
                .build();
    }

    public static void save(VersionSetRevision revision, Path workspaceRoot) throws IOException {
        Preconditions.checkNotNull(revision);
        Preconditions.checkNotNull(workspaceRoot);

        Path tempDir = workspaceRoot.resolve(WorkspaceConstants.TEMP_FOLDER);
        if (Files.notExists(tempDir)) {
            Files.createDirectory(tempDir);
        }

        Path cacheFile = tempDir.resolve(WorkspaceConstants.VERSION_SET_REVISION_CACHE);
        if (Files.exists(cacheFile)) {
            log.warn("Version-set revision file '{}' already exists, we will override it", cacheFile.toString());
        }

        VersionSetRevisionSerializer vsrs = VersionSetRevisionSerializer.convert(revision);
        mapper.writeValue(cacheFile.toFile(), vsrs);
    }

    public static VersionSetRevision load(Path workspaceRoot) throws IOException, VersionSetNotSyncedException {
        Preconditions.checkNotNull(workspaceRoot);

        Path revisionCacheFile = workspaceRoot
                .resolve(WorkspaceConstants.TEMP_FOLDER)
                .resolve(WorkspaceConstants.VERSION_SET_REVISION_CACHE);
        if (Files.notExists(revisionCacheFile)) {
            throw new VersionSetNotSyncedException();
        }

        VersionSetRevisionSerializer vsrs = mapper.readValue(revisionCacheFile.toFile(), VersionSetRevisionSerializer.class);
        return VersionSetRevisionSerializer.convert(vsrs);
    }

    public static void clear(Path workspaceRoot) throws IOException {
        Preconditions.checkNotNull(workspaceRoot);

        Path revisionCacheFile = workspaceRoot
                .resolve(WorkspaceConstants.TEMP_FOLDER)
                .resolve(WorkspaceConstants.VERSION_SET_REVISION_CACHE);
        Files.deleteIfExists(revisionCacheFile);
    }
}
