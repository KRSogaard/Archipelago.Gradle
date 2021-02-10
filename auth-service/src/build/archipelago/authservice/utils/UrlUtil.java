package build.archipelago.authservice.utils;

import java.util.List;

public class UrlUtil {
    public static boolean checkRedirectUrl(String redirectUrl, List<String> allowedUrls) {
        for (String allowed : allowedUrls) {
            if (redirectUrl.startsWith(allowed)) {
                return true;
            }
        }
        return false;
    }
}
