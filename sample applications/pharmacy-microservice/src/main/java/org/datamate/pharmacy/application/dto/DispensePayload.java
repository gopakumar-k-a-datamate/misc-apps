package org.datamate.pharmacy.application.dto;

public record DispensePayload(
        String drugCategory,
        int patientAge,
        String doctorSpecialty,
        String clinicId,
        int dispenseQuantity,
        boolean requiresInsuranceApproval
) {
}
