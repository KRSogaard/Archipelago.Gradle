package build.archipelago.authservice.utils;

import java.util.Random;

public class RandomUtil {
    private static final Random random = new Random();

    public String getRandomString() {
        return getRandomString(10);
    }
    public String getRandomString(int targetStringLength) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
