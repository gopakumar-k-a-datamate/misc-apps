-- Insert condition fields for pharmacy:dispensation:execute (Permission ID: 4)

INSERT INTO pharmacy.authz_condition_field (permission_id, field_name, field_type, display_name)
VALUES
(4, 'drugCategory', 'STRING', 'Drug Category'),
(4, 'patientAge', 'NUMBER', 'Patient Age'),
(4, 'doctorSpecialty', 'STRING', 'Doctor Specialty'),
(4, 'clinicId', 'STRING', 'Clinic ID'),
(4, 'dispenseQuantity', 'NUMBER', 'Dispense Quantity'),
(4, 'requiresInsuranceApproval', 'BOOLEAN', 'Requires Insurance Approval')
ON CONFLICT DO NOTHING;
