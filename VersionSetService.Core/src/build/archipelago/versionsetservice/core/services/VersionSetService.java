package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.exceptions.*;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;

import java.util.*;

public interface VersionSetService {
    VersionSet get(String accountId, String versionSetName) throws VersionSetDoseNotExistsException;

    void create(String accountId, String versionSetName, Optional<ArchipelagoPackage> target, final Optional<String> parent) throws VersionSetExistsException;

    String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages) throws VersionSetDoseNotExistsException;

    VersionSetRevision getRevision(String accountId, String versionSetName, String revision) throws VersionSetDoseNotExistsException;

    List<VersionSet> getAll(String accountId);

    void update(String accountId, String versionSetName, UpdateVersionSetRequest request);
}
