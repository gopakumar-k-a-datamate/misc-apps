package org.datamate.pharmacy.application.exception;

public class MedicationNotFoundException extends ApplicationException {

    private static final String ERROR_CODE = "pharmacy.invalid.medication.id";

    public MedicationNotFoundException(String medicationId) {
        super(ERROR_CODE, "Medication with ID '" + medicationId + "' was not found.");
    }
}
