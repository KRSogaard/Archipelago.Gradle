package build.archipelago.maui.common.workspace;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Data
@SuperBuilder
@Slf4j
public class Workspace {
    protected String versionSet;
    protected List<ArchipelagoPackage> localPackages;

    protected Workspace() {
        versionSet = null;
        localPackages = new ArrayList<>();
    }
}
