package build.archipelago.packageservice.models.rest;

import build.archipelago.common.ArchipelagoBuiltPackage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ArchipelagoBuiltPackageRestResponse {
    private String name;
    private String version;
    private String hash;

    public static ArchipelagoBuiltPackageRestResponse from(ArchipelagoBuiltPackage pkg) {
        return ArchipelagoBuiltPackageRestResponse.builder()
                .name(pkg.getName())
                .version(pkg.getVersion())
                .hash(pkg.getHash())
                .build();
    }

    public ArchipelagoBuiltPackage toInternal() {
        return new ArchipelagoBuiltPackage(getName(), getVersion(), getHash());
    }
}
