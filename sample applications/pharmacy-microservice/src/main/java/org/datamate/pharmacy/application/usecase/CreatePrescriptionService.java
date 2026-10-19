package org.datamate.pharmacy.application.usecase;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.CreatePrescriptionPolicyResource;
import org.datamate.pharmacy.application.dto.CreatePrescriptionRequest;
import org.datamate.pharmacy.application.dto.PatientDto;
import org.datamate.pharmacy.application.dto.PractitionerDto;
import org.datamate.pharmacy.application.port.out.PatientPort;
import org.datamate.pharmacy.application.port.out.PractitionerPort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Application Use Case for creating a prescription.
 * Demonstrates 'Pattern 2: Spring Security @PreAuthorize Bridge'.
 * The authorization logic is delegated to a separate Spring bean via SpEL,
 * keeping this business use case completely clean of authorization code.
 */
@Service
public class CreatePrescriptionService {

    private final PractitionerPort practitionerPort;
    private final PatientPort patientPort;
    private final PolicyEnforcer enforcer;

    public CreatePrescriptionService(PractitionerPort practitionerPort,
                                     PatientPort patientPort,
                                     PolicyEnforcer enforcer) {
        this.practitionerPort = practitionerPort;
        this.patientPort = patientPort;
        this.enforcer = enforcer;
    }


    @PreAuthorize("@prescriptionAuthorizor.prescriptionCreate(#payload)")
    public String createPrescription(CreatePrescriptionRequest payload) {
        return "Prescription created ";
    }
}
