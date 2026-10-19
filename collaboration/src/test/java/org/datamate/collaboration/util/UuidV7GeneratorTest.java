package org.datamate.collaboration.util;
import org.datamate.collaboration.chat.domain.model.UuidV7Generator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UUIDv7 Generator Unit Tests")
class UuidV7GeneratorTest {

    @Test
    @DisplayName("should generate valid RFC 9562 Version 7 and Variant 2 UUID")
    void shouldGenerateValidUuidV7() {
        UUID uuid = UuidV7Generator.generate();

        assertThat(uuid).isNotNull();
        assertThat(uuid.version()).isEqualTo(7);
        assertThat(uuid.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("should generate time-ordered sequential UUIDs")
    void shouldGenerateTimeOrderedUuids() throws InterruptedException {
        UUID first = UuidV7Generator.generate();
        Thread.sleep(2);
        UUID second = UuidV7Generator.generate();

        assertThat(first.version()).isEqualTo(7);
        assertThat(second.version()).isEqualTo(7);
        assertThat(first.compareTo(second)).isLessThan(0);
    }
}