package build.archipelago.versionsetservice.core.delegates.deleteCallback;

import build.archipelago.common.versionset.VersionSetCallback;
import build.archipelago.versionsetservice.core.services.VersionSetService;

import java.util.List;

public class DeleteCallbackDelegate {

    private VersionSetService versionSetService;

    public DeleteCallbackDelegate(VersionSetService versionSetService) {
        this.versionSetService = versionSetService;
    }

    public void deleteCallback(String accountId, String versionSet, String id) {
        versionSetService.removeCallback(accountId, versionSet, id);
    }
}
