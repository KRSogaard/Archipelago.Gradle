package build.archipelago.versionsetservice.core.delegates;

import build.archipelago.common.versionset.VersionSet;
import build.archipelago.versionsetservice.core.services.VersionSetService;

import java.util.List;

public class GetVersionSetsDelegate {

    private VersionSetService versionSetService;

    public GetVersionSetsDelegate(VersionSetService versionSetService) {

        this.versionSetService = versionSetService;
    }

    public List<VersionSet> getVersionSets(String accountId) {
        return this.versionSetService.getAll(accountId);
    }
}
