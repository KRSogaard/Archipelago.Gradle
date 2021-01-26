package build.archipelago.harbor.models.versionset;

import build.archipelago.common.ArchipelagoPackage;
import com.google.common.base.*;
import lombok.*;

@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class AddTargetRestRequest {
    private String target;

    public void validate() {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(target));
        ArchipelagoPackage.parse(target);
    }
}
