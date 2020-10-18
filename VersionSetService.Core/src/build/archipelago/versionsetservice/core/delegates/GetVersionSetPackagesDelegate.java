package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.exceptions.VersionSetDoseNotExistsException;
import build.archipelago.common.versionset.VersionSetRevision;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.core.utils.NameUtil;
import com.google.common.base.*;

public class GetVersionSetPackagesDelegate {

    private VersionSetService versionSetService;

    public GetVersionSetPackagesDelegate(VersionSetService versionSetService) {
        this.versionSetService = versionSetService;
    }

    public VersionSetRevision getPackages(String accountId, String versionSetName, String revision)
            throws VersionSetDoseNotExistsException {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(accountId), "An account id is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(versionSetName), "A version set name is required");
        Preconditions.checkArgument(NameUtil.validateVersionSetName(versionSetName), "Version set name was invalid");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(revision), "A version set revision id is required");

        return versionSetService.getRevision(accountId, versionSetName, revision);
    }
}
