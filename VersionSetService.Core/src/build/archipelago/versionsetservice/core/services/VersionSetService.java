package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.*;
import build.archipelago.common.exceptions.*;
import build.archipelago.common.versionset.*;

import java.util.*;

public interface VersionSetService {
    VersionSet get(String versionSetName);
    void create(String versionSetName, List<ArchipelagoPackage> targets, Optional<String> parent) throws VersionSetExistsException;
    String createRevision(String versionSetName, List<ArchipelagoBuiltPackage> parsePackages) throws VersionSetDoseNotExistsException;
    VersionSetRevision getRevision(String versionSetName, String revision) throws VersionSetDoseNotExistsException;
}
