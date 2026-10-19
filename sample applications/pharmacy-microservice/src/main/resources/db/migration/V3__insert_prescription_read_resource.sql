-- Insert read permission for prescription resource
-- Note: Resource ID 2 (pharmacy prescription) was created in V3

INSERT INTO pharmacy.authz_permission (id, resource_id, action, code, description)
VALUES (3, 2, 'read', 'pharmacy:prescription:read', 'Read Prescriptions')
ON CONFLICT DO NOTHING;

-- No conditions added to authz_condition_field for this permission!
