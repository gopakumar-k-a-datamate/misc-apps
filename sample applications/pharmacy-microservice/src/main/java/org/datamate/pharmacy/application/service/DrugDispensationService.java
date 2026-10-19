package org.datamate.pharmacy.application.service;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.DispensePayload;
import org.datamate.pharmacy.application.dto.DrugDispensationPolicyResource;
import org.springframework.stereotype.Service;

@Service
public class DrugDispensationService {

    private final PolicyEnforcer policyEnforcer;

    public DrugDispensationService(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    public String dispenseDrug(DispensePayload payload) {
        // Enforce the fine-grained ABAC policy using the resource payload
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource();
        resource.setDrugCategory(payload.drugCategory());
        resource.setDispenseQuantity(payload.dispenseQuantity());
        resource.setClinicId(payload.clinicId());
        resource.setPatientAge(payload.patientAge());
        resource.setRequiresInsuranceApproval(payload.requiresInsuranceApproval());
        resource.setDoctorSpecialty(payload.doctorSpecialty());
        policyEnforcer.enforce(resource);

        // If we reach here, OPA allowed the dispensation.
        return "Successfully dispensed " + resource.getDispenseQuantity() + " units of " + resource.getDrugCategory() + " drug.";
    }

    // Explicitly unannotated class for testing Fail-Closed edge case
    public String testUnannotatedObject(Object unannotatedResource) {
        policyEnforcer.enforce(unannotatedResource);
        return "This should never be reached!";
    }
}
