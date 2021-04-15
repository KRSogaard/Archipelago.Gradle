package build.archipelago.versionsetservice.core.delegates.getCallbacks;

import build.archipelago.common.versionset.VersionSetCallback;
import build.archipelago.versionsetservice.core.services.VersionSetService;

import java.util.List;

public class GetCallbacksDelegate {

    private VersionSetService versionSetService;

    public GetCallbacksDelegate(VersionSetService versionSetService) {
        this.versionSetService = versionSetService;
    }

    public List<VersionSetCallback> getCallbacks(String accountId, String versionSet) {
        return versionSetService.getCallbacks(accountId, versionSet);
    }
}
