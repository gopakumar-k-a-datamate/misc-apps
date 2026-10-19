-- Register quantity, assignedLocation, user.department, and allowedDepartments condition fields for pharmacy:medication:dispense permission
INSERT INTO authz_condition_field (permission_id, field_name, field_type, display_name)
VALUES 
  ((SELECT id FROM authz_permission WHERE code = 'pharmacy:medication:dispense'), 'quantity', 'NUMBER', 'Quantity'),
  ((SELECT id FROM authz_permission WHERE code = 'pharmacy:medication:dispense'), 'assignedLocation', 'STRING', 'Assigned Location'),
  ((SELECT id FROM authz_permission WHERE code = 'pharmacy:medication:dispense'), 'user.department', 'STRING', 'User Department'),
  ((SELECT id FROM authz_permission WHERE code = 'pharmacy:medication:dispense'), 'allowedDepartments', 'STRING', 'Allowed Departments')
ON CONFLICT DO NOTHING;
