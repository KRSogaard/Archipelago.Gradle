package build.archipelago.versionsetservice.core.services;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.exceptions.VersionSetExistsException;
import build.archipelago.common.versionset.VersionSet;
import build.archipelago.common.versionset.VersionSetRevision;

import java.util.List;
import java.util.Optional;

public interface VersionSetService {
    VersionSet get(String accountId, String versionSetName);
    void create(String accountId, String versionSetName, List<ArchipelagoPackage> targets, Optional<String> parent) throws VersionSetExistsException;
    String createRevision(String accountId, String versionSetName, List<ArchipelagoBuiltPackage> parsePackages) throws VersionSetDoseNotExistsException;
    VersionSetRevision getRevision(String accountId, String versionSetName, String revision) throws VersionSetDoseNotExistsException;
}
