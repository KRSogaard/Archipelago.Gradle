package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.common.versionset.VersionSetCallback;
import build.archipelago.common.versionset.VersionSetRevision;
import com.google.common.base.Strings;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class VersionSetCallbackRestResponse {
    private String id;
    private String url;

    public VersionSetCallback toInternal() {
        return VersionSetCallback.builder()
                .id(getId())
                .url(getUrl())
                .build();
    }

    public static VersionSetCallbackRestResponse form(VersionSetCallback versionSetCallback) {
        return VersionSetCallbackRestResponse.builder()
                .id(versionSetCallback.getId())
                .url(versionSetCallback.getUrl())
                .build();
    }
}
