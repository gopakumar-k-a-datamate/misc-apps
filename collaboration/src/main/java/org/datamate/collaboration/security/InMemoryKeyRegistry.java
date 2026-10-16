package org.datamate.collaboration.security;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link KeyRegistry}.
 * <p>
 * Supports both static configuration loading via {@link ChatTicketProperties} (Option A)
 * and programmatic dynamic key registration (Option B extension point).
 * Maintains a thread-safe registry of public keys keyed by Key ID ('kid'),
 * along with associated issuer metadata.
 */
@Component
public class InMemoryKeyRegistry implements KeyRegistry {

    @EnableLogger
    private Logger logger;

    private final Map<String, PublicKey> keys = new ConcurrentHashMap<>();
    private final Map<String, String> kidToIssuer = new ConcurrentHashMap<>();
    private final Set<String> knownIssuers = ConcurrentHashMap.newKeySet();

    private final ChatTicketProperties chatTicketProperties;

    public InMemoryKeyRegistry(@Autowired(required = false) ChatTicketProperties chatTicketProperties) {
        this.chatTicketProperties = chatTicketProperties;
    }

    @PostConstruct
    public void initStaticKeys() {
        if (chatTicketProperties != null && chatTicketProperties.getIssuers() != null) {
            for (ChatTicketProperties.IssuerConfig issuerConfig : chatTicketProperties.getIssuers()) {
                String kid = issuerConfig.getKid();
                String issuer = issuerConfig.getIssuer();
                String publicKeyPem = issuerConfig.getPublicKey();

                if (kid != null && !kid.isBlank() && publicKeyPem != null && !publicKeyPem.isBlank()) {
                    try {
                        registerKey(kid, issuer, publicKeyPem);
                        if (logger != null) {
                            logger.info("Loaded static public key for issuer: [{}] with kid: [{}]", issuer, kid);
                        }
                    } catch (Exception e) {
                        if (logger != null) {
                            logger.error("Failed to load static public key for kid: [{}] from configuration", kid, e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Optional<PublicKey> getKey(String kid) {
        if (kid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(keys.get(kid));
    }

    @Override
    public void registerKey(String kid, PublicKey publicKey) {
        registerKey(kid, null, publicKey);
    }

    @Override
    public void registerKey(String kid, String base64PublicKey) {
        registerKey(kid, null, base64PublicKey);
    }

    @Override
    public void registerKey(String kid, String issuer, PublicKey publicKey) {
        if (kid == null || publicKey == null) {
            throw new IllegalArgumentException("kid and publicKey must not be null");
        }
        keys.put(kid, publicKey);
        if (issuer != null && !issuer.isBlank()) {
            kidToIssuer.put(kid, issuer);
            knownIssuers.add(issuer);
        }
        if (logger != null) {
            logger.info("Successfully registered public key for kid: [{}] (issuer: [{}])", kid, issuer != null ? issuer : "unspecified");
        }
    }

    @Override
    public void registerKey(String kid, String issuer, String base64PublicKey) {
        if (base64PublicKey == null || base64PublicKey.isBlank()) {
            throw new IllegalArgumentException("Public key string must not be null or blank");
        }
        try {
            String pem = base64PublicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] encoded = Base64.getDecoder().decode(pem);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey pubKey = keyFactory.generatePublic(keySpec);

            registerKey(kid, issuer, pubKey);
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Failed to parse public key format for kid: [{}]", kid, e);
            }
            throw new IllegalArgumentException("Invalid public key format for kid: " + kid, e);
        }
    }

    @Override
    public Optional<String> getIssuerForKid(String kid) {
        if (kid == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(kidToIssuer.get(kid));
    }

    @Override
    public boolean isKnownIssuer(String issuer) {
        // If no specific issuers have been configured or registered, accept any non-blank issuer (e.g. dev/test mode)
        if (knownIssuers.isEmpty()) {
            return issuer != null && !issuer.isBlank();
        }
        return issuer != null && knownIssuers.contains(issuer);
    }
}
