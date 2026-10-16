package org.datamate.collaboration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTicketPropertiesTest {

    @Test
    @DisplayName("InMemoryKeyRegistry should automatically load static issuers configured in ChatTicketProperties")
    void testStaticIssuerConfigurationLoading() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair1 = gen.generateKeyPair();
        KeyPair pair2 = gen.generateKeyPair();

        String pub1Base64 = Base64.getEncoder().encodeToString(pair1.getPublic().getEncoded());
        String pub2Base64 = Base64.getEncoder().encodeToString(pair2.getPublic().getEncoded());

        ChatTicketProperties.IssuerConfig issuer1 = new ChatTicketProperties.IssuerConfig();
        issuer1.setIssuer("restaurant-service");
        issuer1.setKid("restaurant-key-v1");
        issuer1.setPublicKey("-----BEGIN PUBLIC KEY-----\n" + pub1Base64 + "\n-----END PUBLIC KEY-----");

        ChatTicketProperties.IssuerConfig issuer2 = new ChatTicketProperties.IssuerConfig();
        issuer2.setIssuer("emr-service");
        issuer2.setKid("emr-key-v1");
        issuer2.setPublicKey(pub2Base64);

        ChatTicketProperties properties = new ChatTicketProperties();
        properties.setIssuers(List.of(issuer1, issuer2));

        InMemoryKeyRegistry registry = new InMemoryKeyRegistry(properties);
        registry.initStaticKeys();

        assertThat(registry.getKey("restaurant-key-v1")).isPresent();
        assertThat(registry.getKey("restaurant-key-v1").get()).isEqualTo(pair1.getPublic());
        assertThat(registry.getIssuerForKid("restaurant-key-v1")).contains("restaurant-service");
        assertThat(registry.isKnownIssuer("restaurant-service")).isTrue();

        assertThat(registry.getKey("emr-key-v1")).isPresent();
        assertThat(registry.getKey("emr-key-v1").get()).isEqualTo(pair2.getPublic());
        assertThat(registry.getIssuerForKid("emr-key-v1")).contains("emr-service");
        assertThat(registry.isKnownIssuer("emr-service")).isTrue();

        assertThat(registry.isKnownIssuer("unregistered-service")).isFalse();
    }
}
