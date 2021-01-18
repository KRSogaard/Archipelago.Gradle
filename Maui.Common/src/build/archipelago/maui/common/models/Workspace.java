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
    protected List<ArchipelagoPackage> targets;
    protected List<String> localPackages;

    protected Workspace() {
        versionSet = null;
        targets = new ArrayList<>();
        localPackages = new ArrayList<>();
    }
}
