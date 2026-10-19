ALTER TABLE role
ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'INACTIVE',
ADD COLUMN created_by VARCHAR(255),
ADD COLUMN created_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN last_modified_by VARCHAR(255),
ADD COLUMN last_modified_date TIMESTAMP WITH TIME ZONE,
ADD COLUMN reference_system VARCHAR(50),
ADD COLUMN reference_value VARCHAR(255),
ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN domain_version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS role_aud (
    id BIGINT NOT NULL,
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

ALTER TABLE role DROP CONSTRAINT IF EXISTS role_name_key;
CREATE UNIQUE INDEX idx_role_name_active_only ON role (LOWER(name)) WHERE deleted_at IS NULL;
