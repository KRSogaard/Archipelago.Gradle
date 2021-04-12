package build.archipelago.common.dynamodb;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public class AV {
    public static AttributeValue of(String v) {
        return new AttributeValue(v);
    }

    public static AttributeValue of(int v) {
        return new AttributeValue().withN(String.valueOf(v));
    }

    public static AttributeValue of(Long v) {
        return new AttributeValue().withN(String.valueOf(v));
    }

    public static AttributeValue of(Instant v) {
        return new AttributeValue().withN(String.valueOf(v.toEpochMilli()));
    }

    public static AttributeValue of(List<String> v) {
        return new AttributeValue().withSS(v);
    }

    public static AttributeValue of(boolean dryRun) {
        return new AttributeValue().withBOOL(dryRun);
    }

    public static Instant toInstant(AttributeValue av) {
        return Instant.ofEpochMilli(Long.parseLong(av.getN()));
    }

    public static Instant toInstantOrNull(Map<String, AttributeValue> map, String key) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return null;
        }
        return toInstant(map.get(key));
    }

    public static <T> T getOrNull(Map<String, AttributeValue> map, String key, Function<AttributeValue, T> parse) {
        if (!map.containsKey(key)) {
            return null;
        }
        return parse.apply(map.get(key));
    }

    public static <T extends Enum<T>> T getEnumOrNull(Map<String, AttributeValue> map, String key, Function<AttributeValue, T> parse) {
        if (!map.containsKey(key)) {
            return null;
        }
        return parse.apply(map.get(key));
    }

    public static String getStringOrNull(Map<String, AttributeValue> map, String key) {
        if (!map.containsKey(key)) {
            return null;
        }
        return map.get(key).getS();
    }

    public static <T> T getOrDefault(Map<String, AttributeValue> map, String key, Function<AttributeValue, T> parse, T defaultValue) {
        if (!map.containsKey(key)) {
            return defaultValue;
        }
        return parse.apply(map.get(key));
    }

    public static String getStringOrDefault(Map<String, AttributeValue> map, String key, String defaultValue) {
        return getOrDefault(map, key, AttributeValue::getS, defaultValue);
    }

    public static String debug(Map<String, AttributeValue> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (String key : map.keySet()) {
            sb.append(key);
            sb.append(":");
            sb.append(" ");
            sb.append(map.get(key).toString());
            sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
