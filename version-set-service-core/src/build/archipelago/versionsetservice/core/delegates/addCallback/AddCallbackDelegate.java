package build.archipelago.versionsetservice.core.delegates.addCallback;

import build.archipelago.versionsetservice.core.services.VersionSetService;
import build.archipelago.versionsetservice.exceptions.VersionSetDoseNotExistsException;

public class AddCallbackDelegate {

    private VersionSetService versionSetService;

    public AddCallbackDelegate(VersionSetService versionSetService) {
        this.versionSetService = versionSetService;
    }

    public void addCallback(String accountId, String versionSet, String url) throws VersionSetDoseNotExistsException {
        versionSetService.get(accountId, versionSet);
        versionSetService.addCallback(accountId, versionSet, url);
    }
}
