package build.archipelago.maui.common.models;

import build.archipelago.common.ArchipelagoPackage;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Data
@SuperBuilder
@Slf4j
public class Workspace {
    protected String versionSet;
    protected ArchipelagoPackage target;
    protected List<String> localPackages;

    protected Workspace() {
        versionSet = null;
        target = null;
        localPackages = new ArrayList<>();
    }
}
