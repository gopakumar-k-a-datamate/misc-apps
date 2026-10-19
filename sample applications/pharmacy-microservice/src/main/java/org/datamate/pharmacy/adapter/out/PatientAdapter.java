package org.datamate.pharmacy.adapter.out;

import org.datamate.pharmacy.application.dto.PatientDto;
import org.datamate.pharmacy.application.port.out.PatientPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PatientAdapter implements PatientPort {

    private final Map<String, PatientDto> database = new HashMap<>();

    public PatientAdapter() {
        database.put("PAT-001", new PatientDto("PAT-001", "John Doe", 25, "CARDIOLOGY"));
        database.put("PAT-002", new PatientDto("PAT-002", "Emma Wilson", 12, "PEDIATRICS"));
        database.put("PAT-003", new PatientDto("PAT-003", "Michael Brown", 18, "GENERAL"));
        database.put("PAT-004", new PatientDto("PAT-004", "Sophia Davis", 65, "CARDIOLOGY"));
        database.put("PAT-005", new PatientDto("PAT-005", "James Anderson", 30, "GENERAL"));
        database.put("PAT-006", new PatientDto("PAT-006", "Olivia Taylor", 8, "PEDIATRICS"));
    }

    @Override
    public PatientDto getPatientById(String patientId) {
        return database.get(patientId);
    }
}
