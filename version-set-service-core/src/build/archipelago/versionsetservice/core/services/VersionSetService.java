package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.exceptions.*;
import build.archipelago.versionsetservice.models.UpdateVersionSetRequest;

import java.util.*;

public interface VersionSetService {
    VersionSet get(String accountId, String versionSetName) throws VersionSetDoseNotExistsException;

    void create(String accountId, String versionSetName, ArchipelagoPackage target, String parent, List<String> callbacks) throws VersionSetExistsException;

    String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> packages, ArchipelagoPackage target) throws VersionSetDoseNotExistsException;

    VersionSetRevision getRevision(String accountId, String versionSetName, String revision) throws VersionSetDoseNotExistsException;

    List<VersionSet> getAll(String accountId);

    List<VersionSetCallback> getCallbacks(String accountId, String versionSetName);
    void removeCallback(String accountId, String versionSetName, String id);
    void addCallback(String accountId, String versionSetName, String url);

    void update(String accountId, String versionSetName, UpdateVersionSetRequest request);
}
