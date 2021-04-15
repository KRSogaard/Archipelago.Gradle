package build.archipelago.versionsetservice.core.delegates.deleteCallback;

import build.archipelago.common.versionset.VersionSetCallback;
import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;

import java.util.List;

public class DeleteCallbackDelegate {

    private VersionSetService versionSetService;

    public DeleteCallbackDelegate(VersionSetService versionSetService) {
        this.versionSetService = versionSetService;
    }

    public void deleteCallback(String accountId, String versionSet, String id) throws VersionSetDoseNotExistsException {
        versionSetService.get(accountId, versionSet);
        versionSetService.removeCallback(accountId, versionSet, id);
    }
}
