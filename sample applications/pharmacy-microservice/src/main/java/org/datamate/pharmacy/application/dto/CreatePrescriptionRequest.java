package org.datamate.pharmacy.application.dto;

public record CreatePrescriptionRequest(
        String practitionerId,
        String patientId,
        String medicine
) { }
