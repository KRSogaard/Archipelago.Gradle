package build.archipelago.authservice.utils;

import build.archipelago.authservice.models.UserCredential;
import org.apache.commons.codec.binary.Base64;

public class HeaderUtil {
    public static UserCredential extractCredential(String authHeader) {
        if (!authHeader.toLowerCase().startsWith("basic ")) {
            return null;
        }
        String[] split;
        split = authHeader.split(" ");
        if (split.length != 2) {
            return null;
        }
        String userPass = new String(Base64.decodeBase64(split[1].getBytes()));

        split = userPass.split(":");
        if (split.length != 2) {
            return null;
        }

        return UserCredential.builder()
                .username(split[0])
                .password(split[1])
                .build();
    }
}
