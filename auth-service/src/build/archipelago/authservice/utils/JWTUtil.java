package build.archipelago.authservice.utils;

import java.util.*;

public class JWTUtil {
    public static Map<String, String> getHeader(String jwt) {
        String[] sections = jwt.split("\\.");
        String head = new String(Base64.getDecoder().decode(sections[0]));
        HashMap<String, String> map = JSONUtil.deserialize(head, HashMap.class);

        return map;
    }
}
