DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_aud;
DROP TABLE IF EXISTS role;

CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'INACTIVE',
    created_by VARCHAR(255),
    created_date TIMESTAMP WITH TIME ZONE,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    reference_system VARCHAR(50),
    reference_value VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    domain_version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_role_name_active_only ON role (LOWER(name)) WHERE deleted_at IS NULL;

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);

CREATE TABLE role_aud (
    id UUID NOT NULL,
    rev INT NOT NULL,
    revtype SMALLINT,
    name VARCHAR(100),
    description VARCHAR(500),
    status VARCHAR(50),
    reference_system VARCHAR(50),
    reference_value VARCHAR(255),
    version BIGINT,
    domain_version BIGINT,
    deleted_at TIMESTAMP,
    CONSTRAINT pk_role_aud PRIMARY KEY (id, rev)
);
