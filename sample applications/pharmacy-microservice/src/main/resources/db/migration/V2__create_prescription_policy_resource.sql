INSERT INTO pharmacy.authz_resource (id, namespace, name, description)

VALUES (2, 'pharmacy', 'prescription', 'Pharmacy Prescription Resource')

ON CONFLICT DO NOTHING;



INSERT INTO pharmacy.authz_permission (id, resource_id, action, code, description)

VALUES (2, 2, 'create', 'pharmacy:prescription:create', 'Create Prescription')

ON CONFLICT DO NOTHING;



INSERT INTO pharmacy.authz_condition_field (permission_id, field_name, field_type, display_name, allowed_values)

VALUES

(2, 'doctorLevel', 'STRING', 'Doctor Level', '["MAIN", "SENIOR", "JUNIOR"]'),

(2, 'isSameWard', 'BOOLEAN', 'Is Same Ward', '["true", "false"]');