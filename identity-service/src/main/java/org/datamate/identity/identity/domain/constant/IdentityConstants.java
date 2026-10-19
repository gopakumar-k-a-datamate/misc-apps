package org.datamate.identity.identity.domain.constant;

import java.util.UUID;

/**
 * Central system constants for core identity roles and baseline users.
 */
public final class IdentityConstants {

    private IdentityConstants() {
        // Prevent instantiation
    }

    // Role IDs
    public static final String POLICY_ADMIN_ROLE_ID_STRING = "00000000-0000-0000-0000-000000000000";
    public static final UUID POLICY_ADMIN_ROLE_ID = UUID.fromString(POLICY_ADMIN_ROLE_ID_STRING);

    public static final String USER_ROLE_ID_STRING = "22222222-2222-2222-2222-222222222222";
    public static final UUID USER_ROLE_ID = UUID.fromString(USER_ROLE_ID_STRING);

    // Role Names
    public static final String ROLE_POLICY_ADMIN = "POLICY_ADMIN";
    public static final String ROLE_USER = "USER";

    // User IDs
    public static final String ADMIN_USER_ID_STRING = "10000000-0000-0000-0000-000000000000";
    public static final UUID ADMIN_USER_ID = UUID.fromString(ADMIN_USER_ID_STRING);

    public static final String DEFAULT_USER_ID_STRING = "20000000-0000-0000-0000-000000000000";
    public static final UUID DEFAULT_USER_ID = UUID.fromString(DEFAULT_USER_ID_STRING);

    // User Names / Emails
    public static final String ADMIN_USERNAME = "admin@123.com";
    public static final String DEFAULT_USERNAME = "user@123.com";
}
