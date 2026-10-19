-- Seed data for pharmacy resources

INSERT INTO pharmacy.authz_resource (id, namespace, name, description)
VALUES (1, 'pharmacy', 'medication', 'Pharmacy Medication Resource')
ON CONFLICT DO NOTHING;

INSERT INTO pharmacy.authz_permission (id, resource_id, action, code, description)
VALUES (1, 1, 'dispense', 'pharmacy:medication:dispense', 'Dispense Medication')
ON CONFLICT DO NOTHING;

INSERT INTO pharmacy.authz_condition_field (permission_id, field_name, field_type, display_name, allowed_values)
VALUES 
(1, 'drugClass', 'STRING', 'Drug Class', '["OTC", "PRESCRIPTION", "CONTROLLED"]'),
(1, 'patientAge', 'NUMBER', 'Patient Age', NULL);

