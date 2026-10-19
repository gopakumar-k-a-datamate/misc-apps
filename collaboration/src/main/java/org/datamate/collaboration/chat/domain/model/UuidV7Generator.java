package org.datamate.collaboration.chat.domain.model;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * RFC 9562 compliant UUIDv7 Generator.
 * <p>
 * Domain-level identifier generation utility.
 * Generates time-ordered, 128-bit UUIDs using:
 * <ul>
 *   <li>48-bit Unix epoch millisecond timestamp (Big-Endian)</li>
 *   <li>4-bit UUID version (0111 = Version 7)</li>
 *   <li>12-bit pseudorandom data (rand_a)</li>
 *   <li>2-bit UUID variant (10 = Variant 2 / RFC 4122/9562)</li>
 *   <li>62-bit pseudorandom data (rand_b)</li>
 * </ul>
 * <p>
 * Eliminates B-Tree index fragmentation and primary key collisions during sync.
 */
public final class UuidV7Generator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
    }

    /**
     * Generates a new RFC 9562 Version 7 UUID.
     *
     * @return a time-ordered UUIDv7 instance
     */
    public static UUID generate() {
        long timestamp = System.currentTimeMillis();

        long mostSigBits = (timestamp << 16)
                | (0x7000L)
                | (RANDOM.nextLong() & 0x0FFFL);

        long leastSigBits = (0x8000000000000000L)
                | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);

        return new UUID(mostSigBits, leastSigBits);
    }
}