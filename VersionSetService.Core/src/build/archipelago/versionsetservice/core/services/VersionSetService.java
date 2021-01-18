package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.*;
import build.archipelago.common.versionset.*;
import build.archipelago.versionsetservice.exceptions.*;

import java.util.*;

public interface VersionSetService {
    VersionSet get(String accountId, String versionSetName) throws VersionSetDoseNotExistsException;

    void create(String accountId, String versionSetName, List<ArchipelagoPackage> targets, Optional<String> parent) throws VersionSetExistsException;

    String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> parsePackages) throws VersionSetDoseNotExistsException;

    VersionSetRevision getRevision(String accountId, String versionSetName, String revision) throws VersionSetDoseNotExistsException;

    List<VersionSet> getAll(String accountId);
}
