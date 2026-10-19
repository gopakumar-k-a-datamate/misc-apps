-- 1. Insert the new Dispensation Resource
INSERT INTO pharmacy.authz_resource (id, namespace, name, description)
VALUES (3, 'pharmacy', 'dispensation', 'Drug Dispensation Resource')
ON CONFLICT DO NOTHING;

-- 2. Insert the Permission
INSERT INTO pharmacy.authz_permission (id, resource_id, action, code, description)
VALUES (4, 3, 'execute', 'pharmacy:dispensation:execute', 'Execute Drug Dispensation')
ON CONFLICT DO NOTHING;

