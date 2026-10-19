-- Create audit table for the user_roles join table
CREATE TABLE IF NOT EXISTS user_roles_aud (
    rev INT NOT NULL,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    revtype SMALLINT,
    PRIMARY KEY (rev, user_id, role_id),
    CONSTRAINT fk_user_roles_aud_revinfo FOREIGN KEY (rev) REFERENCES audit.revinfo(id)
);
