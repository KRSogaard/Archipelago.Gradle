package build.archipelago.packageservice.core.utils;

import build.archipelago.packageservice.core.data.PackageData;
import build.archipelago.packageservice.exceptions.PackageNotFoundException;
import build.archipelago.packageservice.models.PackageDetails;

import java.util.regex.*;

public class PackageUtil {
    private final static Pattern re = Pattern.compile("[^A-Za-z0-9]+");

    public static boolean validateHash(String name) {
        Matcher m = re.matcher(name);
        return !m.find();
    }
}
