package org.datamate.pharmacy.application.dto;

public record PatientDto(
    String id,
    String name,
    int age,
    String ward
) {}
