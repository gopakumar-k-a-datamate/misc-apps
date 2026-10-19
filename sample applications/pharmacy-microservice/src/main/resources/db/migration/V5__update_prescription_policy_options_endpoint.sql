-- V6__update_prescription_policy_options_endpoint.sql

UPDATE pharmacy.authz_condition_field 
SET options_endpoint = '/api/v1/pharmacy/doctors',
    allowed_values = NULL
WHERE permission_id = 2 AND field_name = 'doctorLevel';
