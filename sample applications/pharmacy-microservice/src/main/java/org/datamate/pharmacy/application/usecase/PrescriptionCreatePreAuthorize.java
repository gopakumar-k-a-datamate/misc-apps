package org.datamate.pharmacy.application.usecase;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.CreatePrescriptionPolicyResource;
import org.datamate.pharmacy.application.dto.CreatePrescriptionRequest;
import org.datamate.pharmacy.application.dto.PatientDto;
import org.datamate.pharmacy.application.dto.PractitionerDto;
import org.datamate.pharmacy.application.port.out.PatientPort;
import org.datamate.pharmacy.application.port.out.PractitionerPort;
import org.springframework.stereotype.Component;

/**
 * Bridge component for Pattern 2 (@PreAuthorize).
 * This bean is invoked via SpEL in {@link CreatePrescriptionService}.
 * It gathers context from ports, builds the policy resource, and calls the PolicyEnforcer.
 */
@Component("prescriptionAuthorizor")
public class PrescriptionCreatePreAuthorize {

    private final PractitionerPort practitionerPort;
    private final PatientPort patientPort;
    private final PolicyEnforcer enforcer;

    public PrescriptionCreatePreAuthorize(PractitionerPort practitionerPort, PatientPort patientPort, PolicyEnforcer enforcer) {
        this.practitionerPort = practitionerPort;
        this.patientPort = patientPort;
        this.enforcer = enforcer;
    }


    //
    public boolean prescriptionCreate(CreatePrescriptionRequest payload) {
        PractitionerDto practioner = practitionerPort.getPractitionerById(payload.practitionerId());

        if(practioner == null) {
            throw new IllegalArgumentException("Practitioner not found");
        }

        CreatePrescriptionPolicyResource resource = new CreatePrescriptionPolicyResource();
        resource.setDoctorLevel(practioner.level());

        PatientDto patient = patientPort.getPatientById(payload.patientId());

        if (patient == null){
            throw new IllegalArgumentException("Patient not found");
        }

        Boolean isSameWard = practioner.ward().equals(patient.ward());
        resource.setIsSameWard(isSameWard);

        enforcer.enforce(resource);
        return true;
    }
}
