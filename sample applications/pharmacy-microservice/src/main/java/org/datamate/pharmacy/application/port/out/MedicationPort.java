package org.datamate.pharmacy.application.port.out;

import org.datamate.pharmacy.application.dto.MedicationDto;

public interface MedicationPort {
    MedicationDto getMedicationById(String medicationId);
    void saveMedication(MedicationDto medication);
}
