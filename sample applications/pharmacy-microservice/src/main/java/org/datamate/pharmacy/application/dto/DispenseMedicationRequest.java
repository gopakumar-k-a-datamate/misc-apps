package org.datamate.pharmacy.application.dto;

public record DispenseMedicationRequest(
    String medicationId,
    String patientId,
    int quantity
) {}
