-- ============================================================
-- V9: Seed Minimal Identity Roles and Users
-- ============================================================

-- 1. Seed Minimal Essential Roles
INSERT INTO role (id, name, description, status, is_system, version, domain_version, created_at, updated_at)
VALUES 
    ('00000000-0000-0000-0000-000000000000', 'POLICY_ADMIN', 'Root Policy Administrator with OPA authoring rights', 'ACTIVE', TRUE, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'USER', 'Standard User Role', 'ACTIVE', TRUE, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE 
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = 'ACTIVE',
    is_system = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- 2. Seed Minimal Essential Users (password: 'password')
INSERT INTO users (id, user_name, email, password_hash, first_name, last_name, status, version, domain_version, created_at, updated_at)
VALUES 
    ('10000000-0000-0000-0000-000000000000', 'admin@123.com', 'admin@123.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'System', 'Admin', 'ACTIVE', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-0000-0000-000000000000', 'user@123.com', 'user@123.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Standard', 'User', 'ACTIVE', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (user_name) DO UPDATE 
SET user_name = EXCLUDED.user_name,
    email = EXCLUDED.email,
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 3. Seed User Roles Mapping
INSERT INTO user_roles (user_id, role_id, created_at)
VALUES 
    ((SELECT id FROM users WHERE user_name = 'admin@123.com'), '00000000-0000-0000-0000-000000000000', CURRENT_TIMESTAMP),
    ((SELECT id FROM users WHERE user_name = 'admin@123.com'), '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP),
    ((SELECT id FROM users WHERE user_name = 'user@123.com'), '22222222-2222-2222-2222-222222222222', CURRENT_TIMESTAMP)
ON CONFLICT (user_id, role_id) DO NOTHING;
