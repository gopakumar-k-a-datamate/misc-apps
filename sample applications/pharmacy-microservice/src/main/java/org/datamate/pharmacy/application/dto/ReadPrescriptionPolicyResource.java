package org.datamate.pharmacy.application.dto;

import org.datamate.authz.annotation.PolicyResource;

@PolicyResource(namespace = "pharmacy", resourceName = "prescription", action = "read", description = "Read prescriptions")
public class ReadPrescriptionPolicyResource {
    // No PolicyFields needed! This demonstrates unconditional or purely role-based access.
}
