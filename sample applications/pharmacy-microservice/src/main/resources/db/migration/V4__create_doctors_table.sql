-- V5__create_doctors_table.sql

CREATE TABLE doctors (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE INDEX idx_doctors_name
    ON doctors(name);

CREATE INDEX idx_doctors_active
    ON doctors(active);

INSERT INTO doctors (id, name, department, created_by)
VALUES
    ('MAIN', 'Main Doctor', 'ICU', 'SYSTEM'),
    ('SENIOR', 'Senior Doctor', 'CARDIOLOGY', 'SYSTEM'),
    ('JUNIOR', 'Junior Doctor', 'CARDIOLOGY', 'SYSTEM'),
    ('JUNIOR-2', 'Junior Doctor', 'GENERAL', 'SYSTEM');
