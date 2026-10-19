package org.datamate.pharmacy.application.exception;

public class PatientNotFoundException extends ApplicationException {

    private static final String ERROR_CODE = "pharmacy.invalid.patient.id";

    public PatientNotFoundException(String patientId) {
        super(ERROR_CODE, "Patient with ID '" + patientId + "' was not found.");
    }
}
