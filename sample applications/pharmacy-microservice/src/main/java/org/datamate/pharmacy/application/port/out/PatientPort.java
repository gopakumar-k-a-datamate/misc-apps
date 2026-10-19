package org.datamate.pharmacy.application.port.out;

import org.datamate.pharmacy.application.dto.PatientDto;

public interface PatientPort {
    PatientDto getPatientById(String patientId);
}
