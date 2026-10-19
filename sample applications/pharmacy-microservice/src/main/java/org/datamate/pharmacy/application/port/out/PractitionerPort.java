package org.datamate.pharmacy.application.port.out;

import org.datamate.pharmacy.application.dto.PractitionerDto;

public interface PractitionerPort {
    PractitionerDto getPractitionerById(String practitionerId);
}
