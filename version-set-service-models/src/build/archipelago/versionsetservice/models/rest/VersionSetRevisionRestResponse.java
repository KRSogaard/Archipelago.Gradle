package build.archipelago.versionsetservice.models.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import build.archipelago.common.ArchipelagoPackage;
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
public class VersionSetRevisionRestResponse {
    private Long created;
    private String target;
    private List<String> packages;

    public VersionSetRevision toInternal() {
        return VersionSetRevision.builder()
                .created(Instant.ofEpochMilli(this.getCreated()))
                .target(Strings.isNullOrEmpty(this.getTarget()) ? null : ArchipelagoPackage.parse(this.getTarget()))
                .packages(this.getPackages().stream().map(ArchipelagoBuiltPackage::parse).collect(Collectors.toList()))
                .build();
    }
}
