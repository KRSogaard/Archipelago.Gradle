package build.archipelago.versionsetservice.core.utils;

import build.archipelago.common.*;

public class TestConstants {
    public static ArchipelagoPackage pkgA = ArchipelagoPackage.parse("TestPackageA-1.0");
    public static ArchipelagoPackage pkgB = ArchipelagoPackage.parse("TestPackageB-1.0");
    public static ArchipelagoPackage pkgC = ArchipelagoPackage.parse("TestPackageC-1.0");
    public static ArchipelagoBuiltPackage pkgABuild = ArchipelagoBuiltPackage.parse(
            pkgA.toString() + ":" + RevisionUtil.getRandomRevisionId());
    public static ArchipelagoBuiltPackage pkgBBuild = ArchipelagoBuiltPackage.parse(
            pkgB.toString() + ":" + RevisionUtil.getRandomRevisionId());
    public static ArchipelagoBuiltPackage pkgCBuild = ArchipelagoBuiltPackage.parse(
            pkgC.toString() + ":" + RevisionUtil.getRandomRevisionId());
}
