package build.archipelago.maui.common.cache;

import build.archipelago.common.ArchipelagoBuiltPackage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MapPackageCacheList implements PackageCacheList {

    private Map<String, ArchipelagoBuiltPackage> map;

    public MapPackageCacheList(List<ArchipelagoBuiltPackage> packageList) {
        map = new HashMap<>();
        for(ArchipelagoBuiltPackage pkg : packageList) {
            String name = getNormalizedName(pkg);
            if (map.containsKey(name)) {
                log.warn("Package {} was in package list more then twice", name);
                continue;
            }
            map.put(name, pkg);
        }
    }

    @Override
    public boolean hasPackage(ArchipelagoBuiltPackage pkg) {
        return map.containsKey(getNormalizedName(pkg));
    }

    private String getNormalizedName(ArchipelagoBuiltPackage pkg) {
        return pkg.getBuiltPackageName().toLowerCase();
    }
}
