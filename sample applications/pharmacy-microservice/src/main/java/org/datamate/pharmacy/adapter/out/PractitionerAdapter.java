package org.datamate.pharmacy.adapter.out;

import org.datamate.pharmacy.application.dto.PractitionerDto;
import org.datamate.pharmacy.application.port.out.PractitionerPort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PractitionerAdapter implements PractitionerPort {

    private final Map<String, PractitionerDto> database = new HashMap<>();

    public PractitionerAdapter() {
        database.put("DOC-MAIN", new PractitionerDto("DOC-MAIN", "MAIN", "ICU"));
        database.put("DOC-SENIOR", new PractitionerDto("DOC-SENIOR", "SENIOR", "CARDIOLOGY"));
        database.put("DOC-JUNIOR-1", new PractitionerDto("DOC-JUNIOR-1", "JUNIOR", "CARDIOLOGY"));
        database.put("DOC-JUNIOR-2", new PractitionerDto("DOC-JUNIOR-2", "JUNIOR", "GENERAL"));
    }

    @Override
    public PractitionerDto getPractitionerById(String practitionerId) {
        return database.get(practitionerId);
    }
}
