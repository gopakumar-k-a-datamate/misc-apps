package org.datamate.pharmacy.application.usecase;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.ReadPrescriptionPolicyResource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application Use Case for reading prescriptions.
 * Demonstrates 'Pattern 3: Unconditional / Pure RBAC'.
 * The {@link ReadPrescriptionPolicyResource} contains no fields,
 * so the OPA policy only checks if the user holds the required role.
 */
@Service
public class ReadPrescriptionService {

    private final PolicyEnforcer policyEnforcer;

    public ReadPrescriptionService(PolicyEnforcer policyEnforcer) {
        this.policyEnforcer = policyEnforcer;
    }

    public List<String> readPrescriptions() {
        // 1. Build Policy Resource (No conditions needed)
        ReadPrescriptionPolicyResource policyResource = new ReadPrescriptionPolicyResource();

        // 2. Enforce OPA Authorization
        policyEnforcer.enforce(policyResource);

        // 3. Return Mock Data
        return List.of(
            "Prescription #101: Aspirin for PAT-001",
            "Prescription #102: Amoxicillin for PAT-002"
        );
    }
}
