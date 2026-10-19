ALTER TABLE role
ADD COLUMN created_by_id UUID,
ADD COLUMN created_by_system VARCHAR(50),
ADD COLUMN created_by_value VARCHAR(255),
ADD COLUMN last_modified_by_id UUID,
ADD COLUMN last_modified_by_system VARCHAR(50),
ADD COLUMN last_modified_by_value VARCHAR(255);

ALTER TABLE role_aud
ADD COLUMN created_by_id UUID,
ADD COLUMN created_by_system VARCHAR(50),
ADD COLUMN created_by_value VARCHAR(255),
ADD COLUMN last_modified_by_id UUID,
ADD COLUMN last_modified_by_system VARCHAR(50),
ADD COLUMN last_modified_by_value VARCHAR(255);
