package build.archipelago.maui.core.workspace;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.core.workspace.models.BuildConfig;

public interface PackageConfigProvider {
    BuildConfig getConfig(ArchipelagoPackage pkg);
}
