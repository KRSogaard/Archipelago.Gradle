package build.archipelago.maui.common;

import build.archipelago.common.ArchipelagoPackage;
import build.archipelago.maui.common.models.BuildConfig;

public interface PackageConfigProvider {
    BuildConfig getConfig(ArchipelagoPackage pkg);
}
