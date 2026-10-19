package org.datamate.pharmacy.adapter.out.persistence;

import org.datamate.pharmacy.application.dto.MedicationDto;
import org.datamate.pharmacy.application.port.out.MedicationPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;

@Component
public class MedicationPersistenceAdapter implements MedicationPort {

    @EnableLogger
    private Logger log;

    private final Map<String, MedicationDto> database = new ConcurrentHashMap<>();

    public MedicationPersistenceAdapter() {
        // Pre-populate for testing
        database.put("MED-001", new MedicationDto("MED-001", "Amoxicillin", "PRESCRIPTION", 100, 20));
        database.put("MED-002", new MedicationDto("MED-002", "Oxycodone", "CONTROLLED", 15, 10));
        database.put("MED-003", new MedicationDto("MED-003", "Ibuprofen", "OTC", 500, 50));
    }

    @Override
    public MedicationDto getMedicationById(String medicationId) {
        return database.get(medicationId);
    }

    @Override
    public void saveMedication(MedicationDto medication) {
        database.put(medication.getId(), medication);
        log.info("[DB] Saved medication state for: {}", medication.getName());
    }
}
