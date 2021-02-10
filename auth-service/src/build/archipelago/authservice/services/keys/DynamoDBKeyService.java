package build.archipelago.authservice.services.keys;

import build.archipelago.authservice.services.DBK;
import build.archipelago.authservice.services.keys.exceptions.KeyNotFoundException;
import build.archipelago.authservice.services.keys.models.*;
import build.archipelago.common.dynamodb.AV;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.google.common.collect.ImmutableMap;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.*;

import java.security.spec.*;
import java.time.Instant;
import java.util.*;

@Slf4j
public class DynamoDBKeyService implements KeyService {
    private final SignatureAlgorithm algorithm = SignatureAlgorithm.RS256;
    private Map<String, KeyDetails> keyHash;

    // TODO this should be config
    private long longestTokenLife = 60 * 60 * 24 * 30;
    private long KeyLifeSpan = longestTokenLife * 2;

    private AmazonDynamoDB dynamoDB;
    private String keysTableName;

    private KeyDetails current;
    private Instant replaceAt;

    public DynamoDBKeyService(AmazonDynamoDB dynamoDB, String keysTableName) {
        this.dynamoDB = dynamoDB;
        this.keysTableName = keysTableName;
        this.keyHash = new HashMap<>();

        loadKeyFromStorage();
    }

    private void loadKeyFromStorage() {
        Instant mustExpiresAfter = Instant.now().plusSeconds(longestTokenLife / 2);
        Optional<JWKKey> bestKey = getActiveKeys().stream()
                .filter(k -> k.getExpiresAt().isAfter(mustExpiresAfter))
                .max(Comparator.comparingLong(k -> k.getExpiresAt().getEpochSecond()));
        JWKKey key = bestKey.orElseGet(this::createNewKey);
        setKeyUsage(key);
    }

    private void setKeyUsage(JWKKey key) {
        current = keyHash.get(key.getKid().toLowerCase());
        replaceAt = key.getExpiresAt().minusSeconds(longestTokenLife);
    }

    private JWKKey createNewKey() {
        KeyPair keyPair = Keys.keyPairFor(algorithm);
        JWKKey key = JWKKey.builder()
                .kid(UUID.randomUUID().toString())
                .privateKey(Encoders.BASE64.encode(keyPair.getPrivate().getEncoded()))
                .publicKey(Encoders.BASE64.encode(keyPair.getPublic().getEncoded()))
                .expiresAt(Instant.now().plusSeconds(KeyLifeSpan))
                .build();

        dynamoDB.putItem(new PutItemRequest(keysTableName, ImmutableMap.<String, AttributeValue>builder()
                .put(DBK.KID, AV.of(key.getKid()))
                .put(DBK.EXPIRES, AV.of(key.getExpiresAt()))
                .put(DBK.PRIVATE_KEY, AV.of(key.getPrivateKey()))
                .put(DBK.PUBLIC_KEY, AV.of(key.getPublicKey()))
                .build()));

        return key;
    }

    @Override
    public KeyDetails getSigningKey() {
        if (replaceAt.isBefore(Instant.now())) {
            loadKeyFromStorage();
        }

        return KeyDetails.builder()
                .keyId(current.getKeyId())
                .algorithm(algorithm.getValue())
                .type("RSA")
                .privatKey(current.getPrivatKey())
                .publicKey(current.getPublicKey())
                .build();
    }

    public List<JWKKey> getActiveKeys() {
        Map<String, KeyDetails> newHash = new HashMap<>();
        List<JWKKey> activeKeys = new ArrayList<>();
        ScanResult result = dynamoDB.scan(new ScanRequest(keysTableName));
        List<WriteRequest> deleteRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            JWKKey key = parseKey(item);
            if (key.getExpiresAt().isBefore(Instant.now())) {
                deleteRequests.add(new WriteRequest(new DeleteRequest(ImmutableMap.<String, AttributeValue>builder()
                        .put(DBK.KID, AV.of(key.getKid()))
                        .build())));
            } else {
                activeKeys.add(key);
                newHash.put(key.getKid().toLowerCase(), parseFromJWK(key));
            }
        }

        keyHash = newHash;
        if (deleteRequests.size() > 0) {
            log.debug("Deleting '{}' expired keys", deleteRequests.size());
            BatchWriteItemRequest deleteRequest = new BatchWriteItemRequest();
            deleteRequest.addRequestItemsEntry(keysTableName, deleteRequests);
            dynamoDB.batchWriteItem(deleteRequest);
        }
        return activeKeys;
    }

    @Override
    public KeyDetails getKey(String keyId) throws KeyNotFoundException {
        if (!keyHash.containsKey(keyId.toLowerCase())) {
            throw new KeyNotFoundException(keyId);
        }
        KeyDetails key = keyHash.get(keyId.toLowerCase());
        if (Instant.now().isAfter(key.getExpiresAt())) {
            keyHash.remove(keyId.toLowerCase());
            throw new KeyNotFoundException(keyId);
        }

        return key;
    }

    private JWKKey parseKey(Map<String, AttributeValue> item) {
        return JWKKey.builder()
                .kid(item.get(DBK.KID).getS())
                .expiresAt(AV.toInstant(item.get(DBK.EXPIRES)))
                .privateKey(item.get(DBK.PRIVATE_KEY).getS())
                .publicKey(item.get(DBK.PUBLIC_KEY).getS())
                .alg(algorithm.getValue())
                .kty("RSA")
                .build();
    }

    private KeyDetails parseFromJWK(JWKKey key) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return KeyDetails.builder()
                .keyId(key.getKid())
                .algorithm(algorithm.getValue())
                .type("RSA")
                .publicKey(kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key.getPublicKey().getBytes(StandardCharsets.UTF_8)))))
                .privatKey(kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key.getPrivateKey().getBytes(StandardCharsets.UTF_8)))))
                .expiresAt(key.getExpiresAt())
                .build();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
